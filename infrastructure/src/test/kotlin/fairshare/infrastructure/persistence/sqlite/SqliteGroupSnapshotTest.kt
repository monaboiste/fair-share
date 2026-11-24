package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupView
import fairshare.domain.group.SettlementCurrency
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class SqliteGroupSnapshotTest :
    ExpectSpec({
        expect("a racing view read sees one committed currency and revision pair") {
            val db = SqliteTestFixture(tempdir()).apply {
                initialize()
                execute("PRAGMA journal_mode = WAL")
            }
            val writer = db.eventStore()
            val opened = writer.append(trip, null, opening).committedRevision()
            val before = writer.append(trip, opened, changes("EUR")).committedRevision()
            val cursorStarted = CountDownLatch(1)
            val updateFinished = CountDownLatch(1)
            val racingReader = SqliteGroupViewRepository(
                db.dataSource().pauseAfterQuery {
                    cursorStarted.countDown()
                    check(updateFinished.await(5, TimeUnit.SECONDS))
                },
            )
            Executors.newSingleThreadExecutor().use { executor ->
                val reading = executor.submit<GroupView?> { racingReader.findById(trip) }
                try {
                    check(cursorStarted.await(5, TimeUnit.SECONDS))
                    val after = writer.append(trip, before, changes("KWD", "JPY")).committedRevision()
                    val afterView = GroupView(trip, tripName, SettlementCurrency("JPY"), after)
                    db.viewRepository().findById(trip) shouldBe afterView
                } finally {
                    updateFinished.countDown()
                }
                reading.get(5, TimeUnit.SECONDS) shouldBe GroupView(trip, tripName, SettlementCurrency("EUR"), before)
            }
        }
    })

private fun DataSource.pauseAfterQuery(pause: () -> Unit): DataSource = object : DataSource by this {
    override fun getConnection(): Connection {
        val connection = this@pauseAfterQuery.connection
        return object : Connection by connection {
            override fun prepareStatement(sql: String): PreparedStatement {
                val statement = connection.prepareStatement(sql)
                return object : PreparedStatement by statement {
                    override fun executeQuery() = statement.executeQuery().also { pause() }
                }
            }
        }
    }
}
