package fairshare.acceptance.support

import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

internal class FairShareClient(private val baseUrl: URI) : AutoCloseable {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    private val json = JsonMapper()

    fun createGroup(name: String, settlementCurrency: String): GroupApiResponse =
        request("POST", "/groups", mapOf("displayName" to name, "settlementCurrency" to settlementCurrency))

    fun getGroup(groupId: String): GroupApiResponse = request("GET", "/groups/$groupId")

    fun changeSettlementCurrency(groupId: String, revision: String, currency: String): GroupApiResponse =
        request("PATCH", "/groups/$groupId/currency", mapOf("settlementCurrency" to currency), revision)

    fun awaitReadiness(timeout: Duration): Boolean {
        val started = System.nanoTime()
        while (System.nanoTime() - started < timeout.toNanos()) {
            val request = HttpRequest.newBuilder(baseUrl.resolve("/actuator/health"))
                .timeout(Duration.ofSeconds(2))
                .build()
            val ready = try {
                http.send(request, BodyHandlers.discarding()).statusCode() == 200
            } catch (_: IOException) {
                false
            }
            if (ready) return true
            Thread.sleep(50)
        }
        return false
    }

    private fun request(
        method: String,
        path: String,
        fields: Map<String, String>? = null,
        revision: String? = null,
    ): GroupApiResponse {
        val request = HttpRequest.newBuilder(baseUrl.resolve(path))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json, application/problem+json")
        val body = if (fields == null) {
            BodyPublishers.noBody()
        } else {
            request.header("Content-Type", "application/json")
            BodyPublishers.ofString(json.writeValueAsString(fields))
        }
        if (revision != null) request.header("If-Match", revision)
        val response = http.send(request.method(method, body).build(), BodyHandlers.ofString())

        return GroupApiResponse(response.statusCode(), response.headers(), json.readTree(response.body()))
    }

    override fun close() {
        http.close()
    }
}
