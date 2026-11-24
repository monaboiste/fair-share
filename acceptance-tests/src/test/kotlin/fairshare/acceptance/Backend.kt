package fairshare.acceptance

import fairshare.bootstrap.FairShareApplication
import io.kotest.matchers.shouldBe
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal fun startBackend(
    database: File,
    databaseProperty: String = "fair-share.persistence.sqlite.jdbc-url",
): Backend = Backend(
    SpringApplicationBuilder(FairShareApplication::class.java).properties(
        mapOf(
            "server.port" to "0",
            "spring.main.banner-mode" to "off",
            "logging.level.root" to "WARN",
            "fair-share.rest.problem-origin" to "https://reference.example.test/",
        ),
    ).run("--$databaseProperty=jdbc:sqlite:$database"),
)

internal class Backend(private val context: ConfigurableApplicationContext) : AutoCloseable by context {
    val origin: String get() = "http://localhost:${context.environment.getRequiredProperty("local.server.port")}"
}

internal val mapper = JsonMapper()

internal fun HttpClient.request(
    method: String,
    url: String,
    json: String = "",
    tag: String? = null,
    accept: String = "application/json",
): HttpResponse<String> {
    val request = HttpRequest.newBuilder(URI(url))
        .header("Content-Type", "application/json")
        .header("Accept", accept)
        .apply { if (tag != null) header("If-Match", tag) }
        .method(method, HttpRequest.BodyPublishers.ofString(json))
        .build()
    return send(request, HttpResponse.BodyHandlers.ofString())
}

internal fun HttpResponse<String>.header(name: String): String = headers().firstValue(name).orElseThrow()

internal fun HttpResponse<String>.shouldMatchGroup(previous: HttpResponse<String>) {
    statusCode() shouldBe 200
    header("ETag") shouldBe previous.header("ETag")
    mapper.readTree(body()) shouldBe mapper.readTree(previous.body())
}
