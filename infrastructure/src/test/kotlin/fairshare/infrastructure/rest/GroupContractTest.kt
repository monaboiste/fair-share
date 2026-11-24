package fairshare.infrastructure.rest

import fairshare.application.group.ChangeGroupSettlementCurrencyUseCase
import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStore
import fairshare.domain.group.GroupId
import fairshare.infrastructure.persistence.store
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.sql.SQLException
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class GroupContractTest :
    BehaviorSpec({
        given("public Group configuration endpoints") {
            val db = tempdir().resolve("groups.db")
            val mvc = groupApi(store("jdbc:sqlite:$db"))

            then("Group snapshots expose only public JSON fields and a comparable strong tag") {
                val initial = mvc.create()
                val path = initial.response.getHeader(HttpHeaders.LOCATION).orEmpty()
                val tag = initial.response.getHeader(HttpHeaders.ETAG)
                val json = initial.body()
                initial.response.status shouldBe 201
                initial.response.getHeader(HttpHeaders.CACHE_CONTROL) shouldBe "no-store"
                json["displayName"].stringValue() shouldBe "Trip"
                json.propertyNames().toSet() shouldBe setOf(
                    "groupId",
                    "displayName",
                    "settlementCurrency",
                    "settlementCurrencyLocked",
                    "participants",
                )
                path shouldBe "/groups/${json["groupId"].stringValue()}"
                json["settlementCurrencyLocked"].asBoolean() shouldBe false
                json["participants"].isArray shouldBe true
                json["participants"].size() shouldBe 0

                val loaded = mvc.perform(get(path)).andReturn()
                loaded.response.status shouldBe 200
                loaded.response.getHeader(HttpHeaders.CACHE_CONTROL) shouldBe "no-store"
                loaded.response.getHeader(HttpHeaders.ETAG) shouldBe tag
                loaded.body() shouldBe json

                val updated = mvc.change(path, tag.orEmpty())
                updated.response.status shouldBe 200
                updated.body()["settlementCurrency"].stringValue() shouldBe "EUR"
                updated.body()["groupId"] shouldBe json["groupId"]
                updated.body()["displayName"] shouldBe json["displayName"]
                val next = updated.response.getHeader(HttpHeaders.ETAG)
                (next != tag) shouldBe true
                mvc.change(path, tag.orEmpty()).assertProblem(412, "group-version-mismatch")
                val fresh = mvc.perform(get(path)).andReturn()
                fresh.response.getHeader(HttpHeaders.ETAG) shouldBe next
                fresh.body() shouldBe updated.body()
            }

            then("missing Groups are addressed-resource failures for both reads and writes") {
                mvc.perform(get("/groups/not-a-group")).andReturn().assertProblem(404, "group-not-found")
                mvc.change("/groups/not-a-group", "\"current\"").assertProblem(404, "group-not-found")
            }

            then("missing If-Match is a precondition failure, while create does not require one") {
                val created = mvc.create()
                created.response.status shouldBe 201
                val path = created.response.getHeader(HttpHeaders.LOCATION).orEmpty()
                val problem = mvc.change(path).assertProblem(428, "precondition-required")
                problem["title"].stringValue() shouldBe "Precondition required"
            }

            then("only a single strong tag is supported, even across header lines") {
                val created = mvc.create()
                val path = created.response.getHeader(HttpHeaders.LOCATION).orEmpty()
                val tag = created.response.getHeader(HttpHeaders.ETAG).orEmpty()
                listOf("", "*", "W/$tag", "unquoted", "$tag, \"other\"", "\"bad tag\"", "$tag suffix").forEach {
                    mvc.change(path, it).assertProblem(400, "invalid-request")
                }
                mvc.change(path, tag, tag).assertProblem(400, "invalid-request")
                mvc.change(path, "\"tag,with,commas\"").assertProblem(412, "group-version-mismatch")
                mvc.change(path, "\"\"").assertProblem(412, "group-version-mismatch")
                mvc.perform(get(path)).andReturn().response.getHeader(HttpHeaders.ETAG) shouldBe tag
            }

            then("the currency patch requires its one supported field and leaves rejected Groups unchanged") {
                val created = mvc.create()
                val path = created.response.getHeader(HttpHeaders.LOCATION).orEmpty()
                val tag = created.response.getHeader(HttpHeaders.ETAG).orEmpty()
                mvc.change(path, tag, json = "{}").assertProblem(400, "invalid-request")
                    .assertField("#", "REQUIRED_FIELD")
                val noBody = patch("$path/currency").header(HttpHeaders.IF_MATCH, tag)
                val missingBody = mvc.perform(noBody).andReturn().assertProblem(400, "invalid-request")
                missingBody.assertField("#", "REQUIRED_FIELD")
                mvc.change(path, tag, json = """{"settlementCurrency":null}""").assertProblem(400, "invalid-request")
                    .assertField("#/settlementCurrency", "INVALID_TYPE")
                mvc.change(path, tag, json = """{"settlementCurrency":"xyz"}""").assertProblem(422, "invalid-group")
                    .assertField("#/settlementCurrency", "INVALID_SETTLEMENT_CURRENCY")
                mvc.perform(get(path)).andReturn().response.getHeader(HttpHeaders.ETAG) shouldBe tag
            }

            then("Group problems keep their media type when a client advertises an unsupported charset") {
                val request = post("/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept("application/json, application/problem+json;charset=ISO-8859-1")
                    .content("{")
                mvc.perform(request).andReturn().assertProblem(400, "invalid-request")
                    .assertField("#", "INVALID_JSON")
            }

            then("interpretable but invalid Group settings are business errors with field pointers") {
                val invalid = mvc.create("""{"displayName":" \t","settlementCurrency":"zzz"}""")
                    .assertProblem(422, "invalid-group")
                invalid.assertField("#/displayName", "BLANK_DISPLAY_NAME")
                invalid.assertField("#/settlementCurrency", "INVALID_SETTLEMENT_CURRENCY")
            }

            then("the backend serves public problem documentation") {
                val doc = mvc.perform(get("/problems/group-version-mismatch")).andReturn().response
                doc.status shouldBe 200
                doc.contentAsString shouldContain "412 Group version mismatch"
                doc.contentAsString shouldContain "This request did not commit"
                mvc.perform(get("/problems/nonexistent-problem")).andReturn().response.status shouldBe 404
            }

            `when`("a Group request fails wire validation") {
                withData(
                    nameFn = { it.description },
                    InvalidJson("absent body", "", "#", "REQUIRED_FIELD"),
                    InvalidJson("null body", "null", "#", "INVALID_TYPE"),
                    InvalidJson("non-object body", "[]", "#", "INVALID_TYPE"),
                    InvalidJson("broken JSON", "{", "#", "INVALID_JSON"),
                    InvalidJson("extra value after body", "$VALID_CREATE {}", "#", "INVALID_JSON"),
                    InvalidJson(
                        "duplicate fields",
                        """{"displayName":"Trip","displayName":"Again","settlementCurrency":"PLN"}""",
                        "#",
                        "INVALID_JSON",
                    ),
                    InvalidJson("required currency absent", """{"displayName":"Trip"}""", "#", "REQUIRED_FIELD"),
                    InvalidJson(
                        "null field",
                        """{"displayName":null,"settlementCurrency":"PLN"}""",
                        "#/displayName",
                        "INVALID_TYPE",
                    ),
                    InvalidJson(
                        "numeric field",
                        """{"displayName":123,"settlementCurrency":"PLN"}""",
                        "#/displayName",
                        "INVALID_TYPE",
                    ),
                    InvalidJson(
                        "unknown property escaped in pointer",
                        """{"displayName":"Trip","settlementCurrency":"PLN","a~/ #":true}""",
                        "#/a~0~1%20%23",
                        "UNSUPPORTED_FIELD",
                    ),
                ) { invalid ->
                    mvc.create(invalid.body).assertProblem(400, "invalid-request")
                        .assertField(invalid.pointer, invalid.code)
                }
            }
        }

        given("a failure reported after a write may already have committed") {
            then("the public error is an unknown outcome, not a rollback or retry") {
                val actual = store("jdbc:sqlite:${tempdir().resolve("outcome.db")}")
                val group = CreateGroupUseCase(actual).execute("Trip", "PLN")
                var attempts = 0
                val failing = object : GroupStore by actual {
                    override fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend {
                        attempts++
                        actual.append(id, expected, batch)
                        throw GroupAppendOutcomeUnknown(SQLException("private storage details"))
                    }
                }
                val response =
                    withLoggedFailure("Group append outcome unknown", GroupAppendOutcomeUnknown::class.java) {
                        groupApi(failing).change("/groups/${group.id.value}", "\"${group.revision.value}\"")
                    }
                response.assertProblem(500, "internal-error")["detail"].stringValue() shouldContain "may have committed"
                response.response.contentAsString shouldNotContain "private storage details"
                attempts shouldBe 1
                GetGroupUseCase(actual).execute(group.id).group.settlementCurrency.code shouldBe "EUR"
            }

            then("other internal failures also produce Problem Details without storage details") {
                val actual = store("jdbc:sqlite:${tempdir().resolve("groups.db")}")
                val failing = object : GroupStore by actual {
                    override fun load(id: GroupId) = error("private diagnostic data")
                }
                val response = withLoggedFailure("Group request failed", IllegalStateException::class.java) {
                    groupApi(failing).perform(get("/groups/any")).andReturn()
                }
                response.assertProblem(500, "internal-error")
                response.response.contentAsString shouldNotContain "private diagnostic data"
            }
        }
    })

private data class InvalidJson(val description: String, val body: String, val pointer: String, val code: String)

private const val VALID_CREATE = """{"displayName":"Trip","settlementCurrency":"PLN"}"""
private const val ORIGIN = "https://docs.example.test"
private val mapper = JsonMapper.builder().enable(
    DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
    DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
).build()
private val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }

private fun groupApi(store: GroupStore): MockMvc = MockMvcBuilders.standaloneSetup(
    GroupController(
        CreateGroupUseCase(store),
        GetGroupUseCase(store),
        ChangeGroupSettlementCurrencyUseCase(store),
        GroupRequestReader(mapper, validator),
    ),
    ProblemDocumentation(),
).setControllerAdvice(GroupProblemAdvice(RestSettings(ORIGIN))).build()

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

private fun MvcResult.body(): JsonNode = mapper.readTree(response.contentAsString)

private fun MvcResult.assertProblem(status: Int, type: String): JsonNode {
    response.status shouldBe status
    response.contentType shouldBe MediaType.APPLICATION_PROBLEM_JSON_VALUE
    response.getHeader(HttpHeaders.CACHE_CONTROL) shouldBe "no-store"
    return body().also { detail ->
        detail["type"].stringValue() shouldBe "$ORIGIN/problems/$type"
        detail["status"].asInt() shouldBe status
        detail["title"].stringValue().isNotBlank() shouldBe true
        detail["detail"].stringValue().isNotBlank() shouldBe true
    }
}

private fun JsonNode.assertField(pointer: String, code: String) {
    this["errors"].any {
        it["pointer"].stringValue() == pointer &&
            it["code"].stringValue() == code &&
            it["detail"].stringValue().isNotBlank()
    } shouldBe true
}

private fun <T> withLoggedFailure(message: String, failureType: Class<out Throwable>, request: () -> T): T {
    val logger = Logger.getLogger(GroupProblemAdvice::class.java.name)
    val records = mutableListOf<LogRecord>()
    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            records += record
        }

        override fun flush() = Unit
        override fun close() = Unit
    }
    logger.addHandler(handler)
    return try {
        val result = request()
        val log = records.single()
        log.level shouldBe Level.SEVERE
        log.message shouldBe message
        log.thrown.javaClass shouldBe failureType
        result
    } finally {
        logger.removeHandler(handler)
    }
}
