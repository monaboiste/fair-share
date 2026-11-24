package fairshare.bootstrap

import jakarta.validation.constraints.AssertTrue
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.URI

@Validated
@ConfigurationProperties("fair-share.rest")
internal data class RestProperties(val problemOrigin: URI) {
    @get:AssertTrue(message = "must be an HTTP or HTTPS origin without a path, credentials, query, or fragment")
    val isProblemOriginValid: Boolean
        get() = problemOrigin.isHttpOrigin() && problemOrigin.hasNoResource()
}

private fun URI.isHttpOrigin(): Boolean =
    ("http".equals(scheme, ignoreCase = true) || "https".equals(scheme, ignoreCase = true)) &&
        host != null && port <= MAX_PORT

private fun URI.hasNoResource(): Boolean =
    userInfo == null && path in setOf("", "/") && query == null && fragment == null

private const val MAX_PORT = 65_535
