package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupRevision
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.events.GroupSettlementCurrencyChanged
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.sql.DataSource

internal class SqliteTestFixture(directory: File) {
    val file = directory.resolve("groups.db")

    fun dataSource(configure: SQLiteConfig.() -> Unit = {}): DataSource = SQLiteDataSource(
        SQLiteConfig().apply {
            setBusyTimeout(5_000)
            setSynchronous(SQLiteConfig.SynchronousMode.FULL)
            enforceForeignKeys(true)
            configure()
        },
    ).apply { url = "jdbc:sqlite:${file.toURI()}" }

    fun initialize() = SqliteSchemaInitializer(dataSource()).initialize()

    fun execute(vararg statements: String) {
        dataSource().connection.use { connection ->
            connection.createStatement().use { command -> statements.forEach(command::execute) }
        }
    }

    fun eventStore(clock: Clock = fixedClock): SqliteGroupEventStore = SqliteGroupEventStore(dataSource(), clock)

    fun viewRepository(): SqliteGroupViewRepository = SqliteGroupViewRepository(dataSource())
}

internal val trip = GroupId("trip")
internal val tripName = GroupName("Weekend trip")
internal val opening = GroupEventBatch(listOf(GroupCreated(tripName, SettlementCurrency("PLN"))))
internal val fixedClock = Clock.fixed(Instant.parse("2026-06-02T00:15:00.123456789Z"), ZoneOffset.UTC)

internal fun changes(vararg code: String) =
    GroupEventBatch(code.map { GroupSettlementCurrencyChanged(SettlementCurrency(it)) })

internal fun GroupAppend.committedRevision(): GroupRevision = (this as GroupAppend.Committed).revision
