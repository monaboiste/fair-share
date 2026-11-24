package fairshare.infrastructure.rest

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fairshare.application.group.ChangeGroupSettlementCurrencyUseCase
import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupEventStore
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupViewRepository
import fairshare.domain.group.GroupId
import fairshare.infrastructure.persistence.sqlite.SqliteGroupEventStore
import fairshare.infrastructure.persistence.sqlite.SqliteGroupViewRepository
import fairshare.infrastructure.persistence.sqlite.SqliteSchemaInitializer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.sqlite.SQLiteDataSource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.sql.SQLException
import java.time.Clock
import javax.sql.DataSource

class GroupContractTest :
    BehaviorSpec({
        lateinit var fixture: RestFixture
        beforeSpec { fixture = RestFixture(dataSource("jdbc:sqlite:${tempdir().resolve("groups.db")}")) }
        afterSpec { fixture.close() }

        given("Group endpoints with the production MVC configuration") {
            then("create, read and update expose only public Group fields and a strong tag") {
                val mvc = fixture.mvc
                val initial = mvc.create()
                val path = initial.header(HttpHeaders.LOCATION)
                val tag = initial.header(HttpHeaders.ETAG)
                initial.response.status shouldBe 201
                initial.header(HttpHeaders.CACHE_CONTROL) shouldBe "no-store"
                initial.body().propertyNames().toSet() shouldBe setOf(
                    "groupId",
                    "displayName",
                    "settlementCurrency",
                    "participants",
                )
                initial.body()["displayName"].stringValue() shouldBe "Trip"
                initial.body()["settlementCurrency"].stringValue() shouldBe "PLN"
                initial.body()["participants"] shouldBe mapper.readTree("[]")
                path shouldBe "/groups/${initial.body()["groupId"].stringValue()}"
                tag.matches(Regex("\"[^\"]+\"")) shouldBe true

                val loaded = mvc.perform(get(path)).andReturn()
                loaded.response.status shouldBe 200
                loaded.header(HttpHeaders.ETAG) shouldBe tag
                loaded.body() shouldBe initial.body()

                val updated = mvc.change(path, tag)
                updated.response.status shouldBe 200
                updated.body()["settlementCurrency"].stringValue() shouldBe "EUR"
                updated.body()["groupId"] shouldBe initial.body()["groupId"]
                updated.body().propertyNames().toSet() shouldBe initial.body().propertyNames().toSet()
                val next = updated.header(HttpHeaders.ETAG)
                (next != tag) shouldBe true
                mvc.change(path, tag).assertProblem(412, "group-version-mismatch")
                val fresh = mvc.perform(get(path)).andReturn()
                fresh.header(HttpHeaders.ETAG) shouldBe next
                fresh.body() shouldBe updated.body()
            }

            then("addressing an absent Group fails both reads and writes") {
                val missing = fixture.mvc.perform(get("/groups/not-a-group")).andReturn()
                missing.assertProblem(404, "group-not-found")["title"].stringValue() shouldBe "Group not found"
                fixture.mvc.change("/groups/not-a-group", "\"current\"").assertProblem(404, "group-not-found")
            }

            then("If-Match syntax is checked before binding and revision before currency business validation") {
                val mvc = fixture.mvc
                val created = mvc.create()
                val path = created.header(HttpHeaders.LOCATION)
                val tag = created.header(HttpHeaders.ETAG)
                mvc.change(path, json = "{").assertProblem(428, "precondition-required")
                val weak = mvc.change(path, "W/$tag", json = "{").assertProblem(400, "invalid-request")
                weak.has("errors") shouldBe false
                mvc.change(path, "\"stale\"", json = "{").assertFieldProblem("#", "INVALID_JSON")
                val badCurrency = """{"settlementCurrency":"xyz"}"""
                mvc.change(path, "\"stale\"", json = badCurrency).assertProblem(412, "group-version-mismatch")
                mvc.change(path, tag, json = badCurrency).assertProblem(422, "invalid-group")
                    .assertField("#/settlementCurrency", "INVALID_SETTLEMENT_CURRENCY")
                mvc.perform(get(path)).andReturn().header(HttpHeaders.ETAG) shouldBe tag
            }

            then("only one strong opaque tag is accepted, including opaque tags containing commas") {
                val mvc = fixture.mvc
                val created = mvc.create()
                val path = created.header(HttpHeaders.LOCATION)
                val tag = created.header(HttpHeaders.ETAG)
                val invalidTags = listOf("", "*", "W/$tag", "unquoted", "$tag, \"other\"", "\"bad tag\"", "$tag suffix")
                for (invalid in invalidTags) {
                    mvc.change(path, invalid).assertProblem(400, "invalid-request")
                }
                mvc.change(path, tag, tag).assertProblem(400, "invalid-request")
                mvc.change(path, "\"tag,with,commas\"").assertProblem(412, "group-version-mismatch")
                mvc.change(path, "\"\"").assertProblem(412, "group-version-mismatch")
            }

            then("PATCH distinguishes empty bodies, absent fields, and explicit nulls") {
                val mvc = fixture.mvc
                val created = mvc.create()
                val path = created.header(HttpHeaders.LOCATION)
                val tag = created.header(HttpHeaders.ETAG)
                val noBody = patch("$path/currency").header(HttpHeaders.IF_MATCH, tag)
                mvc.perform(noBody).andReturn().assertFieldProblem("#", "REQUIRED_FIELD")
                mvc.change(path, tag, json = "{}").assertFieldProblem("#", "REQUIRED_FIELD")
                mvc.change(path, tag, json = "null").assertFieldProblem("#", "INVALID_TYPE")
                mvc.change(path, tag, json = """{"settlementCurrency":null}""")
                    .assertFieldProblem("#/settlementCurrency", "INVALID_TYPE")
                mvc.perform(get(path)).andReturn().header(HttpHeaders.ETAG) shouldBe tag
            }

            then("business validation identifies blank names and unsupported currency codes") {
                fixture.mvc.create("""{"displayName":" \t","settlementCurrency":"PLN"}""")
                    .assertProblem(422, "invalid-group").assertField("#/displayName", "BLANK_DISPLAY_NAME")
                fixture.mvc.create("""{"displayName":"Trip","settlementCurrency":"zzz"}""")
                    .assertProblem(422, "invalid-group")
                    .assertField("#/settlementCurrency", "INVALID_SETTLEMENT_CURRENCY")
            }

            then("problems have stable documentation and media type") {
                val mvc = fixture.mvc
                val doc = mvc.perform(get("/problems/group-version-mismatch")).andReturn().response
                doc.status shouldBe 200
                doc.contentAsString shouldContain "412 Group version mismatch"
                doc.contentAsString shouldContain "This request did not commit"
                mvc.perform(get("/problems/nonexistent-problem")).andReturn().response.status shouldBe 404
                val request = post("/groups").contentType(MediaType.APPLICATION_JSON)
                    .accept("application/json, application/problem+json;charset=ISO-8859-1").content("{")
                mvc.perform(request).andReturn().assertFieldProblem("#", "INVALID_JSON")
            }

            `when`("create receives an invalid request body") {
                withData(
                    nameFn = { it.description },
                    InvalidJson("absent body", "", "#", "REQUIRED_FIELD"),
                    InvalidJson("null body", "null", "#", "INVALID_TYPE"),
                    InvalidJson("non-object body", "[]", "#", "INVALID_TYPE"),
                    InvalidJson("whitespace without JSON", "  ", "#", "INVALID_JSON"),
                    InvalidJson("broken JSON", "{", "#", "INVALID_JSON"),
                    InvalidJson("extra value after body", "$VALID_CREATE {}", "#", "INVALID_JSON"),
                    InvalidJson(
                        "duplicate fields",
                        """{"displayName":"Trip","displayName":"Again"}""",
                        "#",
                        "INVALID_JSON",
                    ),
                    InvalidJson("required currency absent", """{"displayName":"Trip"}""", "#", "REQUIRED_FIELD"),
                    InvalidJson("required name absent", """{"settlementCurrency":"PLN"}""", "#", "REQUIRED_FIELD"),
                    invalidNameType("null field", "null"),
                    invalidNameType("integer field", "123"),
                    invalidNameType("boolean field", "false"),
                    invalidNameType("decimal field", "1.2"),
                    InvalidJson(
                        "unknown property escaped in pointer",
                        """{"displayName":"Trip","settlementCurrency":"PLN","a~/ #":true}""",
                        "#/a~0~1%20%23",
                        "UNSUPPORTED_FIELD",
                    ),
                ) { invalid ->
                    fixture.mvc.create(invalid.body).assertFieldProblem(invalid.pointer, invalid.code)
                }
            }
        }

        given("an internal failure servicing a Group request") {
            then("a write that may have committed returns a safe problem rather than pretending it rolled back") {
                val db = dataSource("jdbc:sqlite:${tempdir().resolve("outcome.db")}")
                val actual = SqliteGroupEventStore(db, Clock.systemUTC())
                val group = CreateGroupUseCase(actual).execute("Trip", "PLN")
                val path = "/groups/${group.id.value}"
                var attempts = 0
                val failing = object : GroupEventStore by actual {
                    override fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend {
                        attempts++
                        actual.append(id, expected, batch)
                        throw GroupAppendOutcomeUnknown(SQLException("private storage details"))
                    }
                }
                RestFixture(db, eventStore = failing).use {
                    val unknown = GroupAppendOutcomeUnknown::class.java
                    val response = withLoggedFailure("Group append outcome unknown", unknown) {
                        it.mvc.change(path, "\"${group.revision.value}\"")
                    }
                    val problem = response.assertProblem(500, "internal-error")
                    problem["detail"].stringValue() shouldContain "may have committed"
                    response.response.contentAsString shouldNotContain "private storage details"
                    attempts shouldBe 1
                    it.mvc.perform(get(path)).andReturn().body()["settlementCurrency"].stringValue() shouldBe "EUR"
                }
            }

            then("other internal failures also hide private diagnostics") {
                val db = dataSource("jdbc:sqlite:${tempdir().resolve("failed-read.db")}")
                val failing = object : GroupViewRepository {
                    override fun findById(id: GroupId) = error("private diagnostic data")
                }
                RestFixture(db, viewRepository = failing).use {
                    val response = withLoggedFailure("Group request failed", IllegalStateException::class.java) {
                        it.mvc.perform(get("/groups/any")).andReturn()
                    }
                    response.assertProblem(500, "internal-error")
                    response.response.contentAsString shouldNotContain "private diagnostic data"
                }
            }
        }
    })

private data class InvalidJson(val description: String, val body: String, val pointer: String, val code: String)

private fun invalidNameType(description: String, value: String): InvalidJson =
    InvalidJson(description, """{"displayName":$value,"settlementCurrency":"PLN"}""", "#/displayName", "INVALID_TYPE")

private const val VALID_CREATE = """{"displayName":"Trip","settlementCurrency":"PLN"}"""
private const val ORIGIN = "https://docs.example.test"
private val mapper = JsonMapper()

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(RestWebConfiguration::class)
private class RestTestConfiguration

private fun dataSource(url: String): DataSource = SQLiteDataSource().apply {
    this.url = url
    SqliteSchemaInitializer(this).initialize()
}

private class RestFixture(
    db: DataSource,
    eventStore: GroupEventStore = SqliteGroupEventStore(db, Clock.systemUTC()),
    viewRepository: GroupViewRepository = SqliteGroupViewRepository(db),
) : AutoCloseable {
    private val context = AnnotationConfigWebApplicationContext().apply {
        servletContext = MockServletContext()
        register(RestTestConfiguration::class.java)
        addBeanFactoryPostProcessor {
            it.registerSingleton("problemOrigin", URI.create(ORIGIN))
            it.registerSingleton("createGroup", CreateGroupUseCase(eventStore))
            it.registerSingleton("getGroup", GetGroupUseCase(viewRepository))
            it.registerSingleton("changeGroup", ChangeGroupSettlementCurrencyUseCase(eventStore))
        }
        refresh()
    }

    val mvc: MockMvc = MockMvcBuilders.webAppContextSetup(context).build()

    override fun close() = context.close()
}

private fun MockMvc.create(json: String = VALID_CREATE): MvcResult =
    perform(post("/groups").contentType(MediaType.APPLICATION_JSON).header("Host", "client-host.test").content(json))
        .andReturn()

private fun MockMvc.change(
    path: String,
    vararg tags: String,
    json: String = """{"settlementCurrency":"EUR"}""",
): MvcResult {
    val request = patch("$path/currency").contentType(MediaType.APPLICATION_JSON).content(json)
    tags.forEach { request.header(HttpHeaders.IF_MATCH, it) }
    return perform(request).andReturn()
}

private fun MvcResult.header(name: String): String = checkNotNull(response.getHeader(name))

private fun MvcResult.body(): JsonNode = mapper.readTree(response.contentAsString)

private fun MvcResult.assertProblem(status: Int, type: String): JsonNode {
    response.status shouldBe status
    response.contentType shouldBe MediaType.APPLICATION_PROBLEM_JSON_VALUE
    header(HttpHeaders.CACHE_CONTROL) shouldBe "no-store"
    return body().also { detail ->
        detail["type"].stringValue() shouldBe "$ORIGIN/problems/$type"
        detail["status"].asInt() shouldBe status
        detail["title"].stringValue() shouldBe ProblemType.entries.single { it.slug == type }.title
        detail["detail"].stringValue().isNotBlank() shouldBe true
        detail["instance"].stringValue() shouldBe request.requestURI
        detail.has("properties") shouldBe false
    }
}

private fun MvcResult.assertFieldProblem(pointer: String, code: String) =
    assertProblem(400, "invalid-request").assertField(pointer, code)

private fun JsonNode.assertField(pointer: String, code: String) {
    val errors = this["errors"].valueStream().toList()
    errors.map { it["pointer"].stringValue() to it["code"].stringValue() } shouldBe listOf(pointer to code)
    errors.single()["detail"].stringValue().isNotBlank() shouldBe true
}

private fun <T> withLoggedFailure(message: String, failureType: Class<out Throwable>, request: () -> T): T {
    val logger = LoggerFactory.getLogger(RestExceptionHandler::class.java) as Logger
    val appender = ListAppender<ILoggingEvent>()
    appender.start()
    logger.addAppender(appender)
    return try {
        val result = request()
        val log = appender.list.single()
        log.level shouldBe Level.ERROR
        log.message shouldBe message
        log.throwableProxy.className shouldBe failureType.name
        result
    } finally {
        logger.detachAppender(appender)
        appender.stop()
    }
}
