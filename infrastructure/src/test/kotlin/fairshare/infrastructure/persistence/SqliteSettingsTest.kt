package fairshare.infrastructure.persistence

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class SqliteSettingsTest :
    ExpectSpec({
        expect("SQLite defaults to a durable local file with extra sync and bounded lock waits") {
            val defaults = SqliteGroupStoreSettings()
            defaults.jdbcUrl shouldBe "jdbc:sqlite:fair-share.db"
            defaults.busyTimeoutMillis shouldBe 5_000
            defaults.synchronous shouldBe SqliteGroupStoreSettings.Synchronous.EXTRA
        }

        expect("only SQLite URLs and non-negative lock waits are accepted") {
            shouldThrow<IllegalArgumentException> { SqliteGroupStoreSettings(jdbcUrl = "jdbc:other") }
            shouldThrow<IllegalArgumentException> { SqliteGroupStoreSettings(busyTimeoutMillis = -1) }
            SqliteGroupStoreSettings(busyTimeoutMillis = 0).busyTimeoutMillis shouldBe 0
        }
    })
