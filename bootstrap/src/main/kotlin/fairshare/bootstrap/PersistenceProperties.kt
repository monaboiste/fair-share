package fairshare.bootstrap

import fairshare.infrastructure.persistence.SqliteGroupStoreSettings
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties("fair-share.persistence")
internal data class PersistenceProperties(@param:DefaultValue val sqlite: SqliteGroupStoreSettings)
