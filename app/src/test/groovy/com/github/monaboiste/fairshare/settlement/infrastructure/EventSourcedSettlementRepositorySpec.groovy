package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.domain.Settlement
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class EventSourcedSettlementRepositorySpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")

    def "a stale Settlement retains pending events after a rejected save"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, EventId::random)
        repository.save(Settlement.open(ID, new SettlementName("Holiday"), EUR, NOW))
        def stale = repository.findById(ID).orElseThrow()
        def winner = repository.findById(ID).orElseThrow()
        winner.rename(new SettlementName("Winner"), NOW)
        repository.save(winner)
        stale.rename(new SettlementName("Loser"), NOW)

        when:
        repository.save(stale)

        then:
        thrown(VersionConflictException)
        stale.pendingEvents().size() == 1
        store.load(ID)*.payload()*.name() == ["Holiday", "Winner"]
    }
}
