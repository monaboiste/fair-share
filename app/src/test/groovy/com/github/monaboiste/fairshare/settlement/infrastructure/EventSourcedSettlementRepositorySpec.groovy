package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class EventSourcedSettlementRepositorySpec extends Specification {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC)
    private static final EUR = Monetary.getCurrency("EUR")

    def "a stale Settlement retains pending events after a rejected save"() {
        given: "two copies of a saved Settlement; one saves a rename to Winner, the other is renamed to Loser"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        SettlementId id = repository.save(Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)).streamId()
        def stale = repository.findById(id).orElseThrow()
        def winner = repository.findById(id).orElseThrow()
        winner.rename(new SettlementName("Winner"))
        repository.save(winner)
        stale.rename(new SettlementName("Loser"))

        when: "the outdated copy is saved"
        repository.save(stale)

        then: "the save fails on a version conflict, the outdated copy keeps its rename and the history is untouched"
        thrown(VersionConflictException)
        stale.pendingEvents().size() == 1
        store.load(id)*.payload()*.name() == ["Holiday", "Winner"]
    }
}
