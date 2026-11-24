package fairshare.acceptance

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.net.http.HttpClient
import java.sql.DriverManager

class LegacyDatabaseHttpTest :
    ExpectSpec({
        expect("a deployed Group survives booting with only the previous database property") {
            val database = tempdir().resolve("legacy.db")
            DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        """
                        CREATE TABLE group_events (
                            group_id TEXT NOT NULL, position INTEGER NOT NULL, revision TEXT NOT NULL,
                            kind TEXT NOT NULL, display_name TEXT, currency TEXT NOT NULL,
                            PRIMARY KEY (group_id, position)
                        )
                        """.trimIndent(),
                    )
                    statement.execute(
                        "INSERT INTO group_events VALUES ('trip', 1, 'v1', 'group-created', 'Trip', 'EUR')",
                    )
                }
            }

            startBackend(database, "fair-share.database-url").use { backend ->
                HttpClient.newHttpClient().use { client ->
                    val group = client.request("GET", "${backend.origin}/groups/trip")
                    group.statusCode() shouldBe 200
                    group.header("ETag") shouldBe "\"v1\""
                    mapper.readTree(group.body())["settlementCurrency"].stringValue() shouldBe "EUR"
                }
            }
        }
    })
