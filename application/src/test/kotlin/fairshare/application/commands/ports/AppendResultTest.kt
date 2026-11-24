package fairshare.application.commands.ports

import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class AppendResultTest : ExpectSpec() {
    init {
        expect("revision zero cannot describe a committed append") {
            shouldThrow<IllegalArgumentException> { AppendResult.Committed(StreamRevision.INITIAL) }
        }

        expect("only a committed append supplies a revision for the next expected-revision check") {
            val outcomes = listOf(AppendResult.RevisionMismatch, AppendResult.Committed(StreamRevision(7)))

            val revisions = outcomes.mapNotNull { outcome ->
                when (outcome) {
                    is AppendResult.Committed -> outcome.revision
                    AppendResult.RevisionMismatch -> null
                }
            }

            revisions shouldBe listOf(StreamRevision(7))
        }
    }
}
