package fairshare.infrastructure.persistence.sqlite

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import javax.sql.DataSource

/** Applies the application changelog to a test database. */
object SqliteEventSchema {
    /** Migrates the database at [dataSource] to the current schema. */
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
