package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupView
import fairshare.application.group.GroupViewRepository
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import javax.sql.DataSource

public class SqliteGroupViewRepository(private val dataSource: DataSource) : GroupViewRepository {
    override fun findById(id: GroupId): GroupView? = readGroupRows(dataSource, READ_VIEW, id) { rows ->
        if (!rows.next()) return@readGroupRows null

        val name: String? = rows.getString("group_name")
        val currency: String? = rows.getString("settlement_currency")
        val revision: String? = rows.getString("group_revision")
        val decoder = GroupEventDecoder(id)
        do {
            decoder.decode(rows)
        } while (rows.next())
        GroupView(
            id,
            GroupName(required(id, name)),
            SettlementCurrency(required(id, currency)),
            GroupRevision(required(id, revision)),
        )
    }

    private fun required(id: GroupId, value: String?): String =
        checkNotNull(value) { "Missing metadata in Group ${id.value} view" }
}
