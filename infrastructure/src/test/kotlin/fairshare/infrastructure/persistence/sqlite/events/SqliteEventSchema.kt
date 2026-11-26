package fairshare.infrastructure.persistence.sqlite.events

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import javax.sql.DataSource

internal object SqliteEventSchema {
    fun initialize(dataSource: DataSource) {
        dataSource.connection.use { connection ->
            ClassLoaderResourceAccessor().use { resources ->
                Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    resources,
                    JdbcConnection(connection),
                ).use { liquibase ->
                    liquibase.update()
                }
            }
        }
    }
}
