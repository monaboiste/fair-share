package fairshare.application.group

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class GroupEventMetadataTest :
    ExpectSpec({
        expect("new event identifiers are nonblank and independent") {
            val eventId = EventId.random()
            eventId.value.isNotBlank() shouldBe true
            (eventId == EventId.random()) shouldBe false
        }

        expect("new revisions are nonblank and independent") {
            val revision = GroupRevision.random()
            revision.value.isNotBlank() shouldBe true
            (revision == GroupRevision.random()) shouldBe false
        }
    })
