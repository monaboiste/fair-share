package com.github.monaboiste.fairshare.settlement

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventStore
import com.github.monaboiste.fairshare.common.events.InMemoryEventStore
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class SourcedSettlementRepositorySpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC)

    def "failed append retains pending events and a version conflict does not retry"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        def repository = new DefaultSourcedSettlementRepository(store, new CommittedSettlementEvents())
        def stale = repository.loadOrCreate(ID)
        stale.open("Original", Monetary.getCurrency("EUR"), CLOCK, UUID::randomUUID)
        def winner = repository.loadOrCreate(ID)
        winner.open("Winner", Monetary.getCurrency("EUR"), CLOCK, UUID::randomUUID)
        repository.save(winner)

        when:
        repository.save(stale)

        then:
        thrown(VersionConflictException)
        stale.loadedVersion() == 0
        stale.incoming()*.payload() == [new SettlementOpened("Original", Monetary.getCurrency("EUR"))]
        store.load(ID)*.payload() == [new SettlementOpened("Winner", Monetary.getCurrency("EUR"))]
    }

    def "publication failure follows commit and reports the committed version"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        def publisher = new CommittedSettlementEvents()
        def received = []
        publisher.subscribe({ EventEnvelope event -> throw new IllegalStateException("projection failed") })
        publisher.subscribe({ EventEnvelope event -> received.add(event) })
        def repository = new DefaultSourcedSettlementRepository(store, publisher)
        def settlement = repository.loadOrCreate(ID)
        settlement.open("Original", Monetary.getCurrency("EUR"), CLOCK, UUID::randomUUID)

        when:
        repository.save(settlement)

        then:
        PostCommitProjectionException failure = thrown()
        failure.committedVersion() == 1
        failure.cause.message == "projection failed"
        settlement.incoming().empty
        settlement.loadedVersion() == 1
        store.load(ID).size() == 1
        received.empty
    }
}
