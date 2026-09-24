package com.github.monaboiste.fairshare.settlement.application.command.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.CLOCK
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.EUR
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.USD

import com.github.monaboiste.fairshare.common.events.EventStore
import com.github.monaboiste.fairshare.common.events.PostCommitPublicationException
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import spock.lang.Specification

class OpenSettlementHandlerSpec extends Specification {
    def configuration = new SettlementTestConfiguration()

    def "opening a Settlement returns its generated identifier and committed opening envelope"() {
        when:
        def result = configuration.openHandler.handle(new OpenSettlement(new SettlementName("  Holiday  "), EUR))

        then:
        def commit = result.getSuccess()
        commit.streamId() != null
        commit.version() == 1
        commit.events().size() == 1
        commit.events().first().streamId() == commit.streamId()
        commit.events().first().sequence() == 1
        commit.events().first().position() == 1
        commit.events().first().eventId() != null
        commit.events().first().occurredAt() == NOW
        commit.events().first().payload() == new SettlementOpened("  Holiday  ", EUR)
    }

    def "opening separate Settlements generates distinct identifiers"() {
        when:
        SettlementId first = configuration.openSettlement("Holiday", EUR)
        SettlementId second = configuration.openSettlement("Mountains", USD)

        then:
        first != null
        second != null
        first != second
        configuration.store.readAll(0)*.streamId() == [first, second]
    }

    def "store failure propagates unchanged without projecting the attempted Settlement"() {
        given:
        def outage = new IllegalStateException("unavailable")
        EventStore<SettlementId, SettlementEvent> broken = Mock()
        def projector = new SettlementProjector()
        SettlementId attemptedId
        def handler = new OpenSettlementHandler(new EventSourcedSettlementRepository(broken, CLOCK), CLOCK)

        when:
        handler.handle(new OpenSettlement(new SettlementName("Holiday"), EUR))

        then:
        1 * broken.append(_, _, _) >> { args ->
            attemptedId = args[0] as SettlementId
            throw outage
        }
        def failure = thrown(IllegalStateException)
        failure.is(outage)
        attemptedId != null
        projector.findById(attemptedId).empty
    }

    def "publication failure leaves the opening committed and reports its identifier and version"() {
        given:
        def failingStore = new InMemoryEventStore<SettlementId, SettlementEvent>()
        failingStore.subscribe { throw new IllegalStateException("projection failed") }
        def projector = new SettlementProjector()
        failingStore.subscribe(projector)
        def handler = new OpenSettlementHandler(new EventSourcedSettlementRepository(failingStore, CLOCK), CLOCK)

        when:
        handler.handle(new OpenSettlement(new SettlementName("Holiday"), EUR))

        then:
        def failure = thrown(PostCommitPublicationException)
        failure.streamId() instanceof SettlementId
        failure.committedVersion() == 1
        failingStore.load(failure.streamId() as SettlementId).size() == 1
        projector.findById(failure.streamId() as SettlementId).empty
    }
}
