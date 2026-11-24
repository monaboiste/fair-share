package fairshare.bootstrap

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties("fair-share.rest")
internal data class RestProperties(@param:DefaultValue("http://localhost:8080") val problemOrigin: String)
