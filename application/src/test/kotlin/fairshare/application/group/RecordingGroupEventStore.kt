package fairshare.application.group

import fairshare.domain.group.GroupId
import io.kotest.matchers.shouldBe
import java.time.Instant

internal class RecordingGroupEventStore : GroupEventStore {
    val streams = mutableMapOf<GroupId, GroupStream>()
    var loadCount = 0
        private set
    var appendCount = 0
        private set
    var rejectNextAppend = false
    var appendFailure: RuntimeException? = null

    override fun load(id: GroupId): GroupStream? {
        loadCount++
        return streams[id]
    }

    override fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend {
        appendCount++
        val stream = streams[id]
        expected shouldBe stream?.revision
        appendFailure?.let { throw it }
        if (rejectNextAppend) {
            rejectNextAppend = false
            return GroupAppend.RevisionMismatch
        }

        val revision = GroupRevision("revision-$appendCount")
        val existing = stream?.events.orEmpty()
        val appended = batch.events.mapIndexed { index, event ->
            val sequence = existing.size.toLong() + index + 1
            GroupEventEnvelope(EventId("event-$sequence"), id, sequence, revision, Instant.EPOCH, event)
        }
        streams[id] = GroupStream(revision, existing + appended)
        return GroupAppend.Committed(revision)
    }
}
