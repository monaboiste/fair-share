package fairshare.application.group

import fairshare.domain.group.GroupId
import io.kotest.matchers.shouldBe

internal class RecordingGroupStore : GroupStore {
    val streams = mutableMapOf<GroupId, GroupStream>()
    var appendCount = 0
        private set
    var rejectNextAppend = false

    override fun load(id: GroupId): GroupStream? = streams[id]

    override fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend {
        appendCount++
        val stream = streams[id]
        expected shouldBe stream?.revision
        if (rejectNextAppend) {
            rejectNextAppend = false
            return GroupAppend.RevisionMismatch
        }
        val revision = GroupRevision("revision-$appendCount")
        streams[id] = GroupStream(revision, stream?.events.orEmpty() + batch.events)
        return GroupAppend.Committed(revision)
    }
}
