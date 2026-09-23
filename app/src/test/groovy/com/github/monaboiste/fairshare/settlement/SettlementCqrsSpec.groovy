package com.github.monaboiste.fairshare.settlement

import com.github.monaboiste.fairshare.common.events.InMemoryEventStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class SettlementCqrsSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")

    def "dispatching an opening makes a projected Settlement available"() {
        given:
        def app = application(new InMemoryEventStore<>())
        def currency = Monetary.getCurrency("EUR")

        when:
        def opened = app.commands().dispatch(new OpenSettlement(ID, "Holiday", currency))

        then:
        opened.getSuccess() == ID
        app.queries().handle(new GetSettlement(ID)) == new SettlementView(ID, "Holiday", currency, 1)
        app.queries().handle(new GetSettlementHistory(ID))*.sequence() == [1L]
    }

    def "renaming replays state and identical openings remain idempotent"() {
        given:
        def store = new InMemoryEventStore<>()
        def app = application(store)
        def currency = Monetary.getCurrency("EUR")
        app.commands().dispatch(new OpenSettlement(ID, "First", currency))

        when:
        app.commands().dispatch(new RenameSettlement(ID, "Second"))
        app.commands().dispatch(new RenameSettlement(ID, "Third"))
        def retried = app.commands().dispatch(new OpenSettlement(ID, "First", currency))
        def noOp = app.commands().dispatch(new RenameSettlement(ID, "Third"))
        def conflict = app.commands().dispatch(new OpenSettlement(ID, "Third", currency))
        def currencyConflict = app.commands().dispatch(new OpenSettlement(ID, "First", Monetary.getCurrency("USD")))

        then:
        retried.getSuccess() == ID
        noOp.getSuccess() == ID
        conflict.getFailure() == new IdentifierConflict(ID)
        currencyConflict.getFailure() == new IdentifierConflict(ID)
        app.queries().handle(new GetSettlement(ID)) == new SettlementView(ID, "Third", currency, 3)
        app.queries().handle(new GetSettlementHistory(ID))*.sequence() == [1L, 2L, 3L]
        Settlement.replay(ID, store.load(ID)).incoming().empty

        when:
        def replayed = Settlement.replay(ID, store.load(ID))
        replayed.rename("Third", Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID)

        then:
        replayed.incoming().empty
        replayed.loadedVersion() == 3
    }

    def "startup replaces projection with all committed streams and ordinary queries do not replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def currency = Monetary.getCurrency("EUR")
        def first = application(store)
        first.commands().dispatch(new OpenSettlement(ID, "Before", currency))
        first.commands().dispatch(new RenameSettlement(ID, "After"))
        def other = new SettlementId(UUID.randomUUID())
        first.commands().dispatch(new OpenSettlement(other, "Other", currency))

        when:
        def projector = new SettlementProjector()
        projector.accept(store.load(ID).first())
        def stale = new SettlementId(UUID.randomUUID())
        projector.accept(new com.github.monaboiste.fairshare.common.events.EventEnvelope(UUID.randomUUID(), stale, 1,
            NOW, new SettlementOpened("Stale", currency)))
        projector.rebuild(store)
        def queries = new SettlementQueryHandler(new com.github.monaboiste.fairshare.common.events.EventStore<SettlementId, SettlementEvent>() {
            List load(SettlementId id) { throw new AssertionError("ordinary query replayed history") }
            com.github.monaboiste.fairshare.common.events.AppendResult append(SettlementId id, long version,
                    List events) { throw new AssertionError("unexpected append") }
        }, projector)

        then:
        queries.handle(new GetSettlement(ID)) == new SettlementView(ID, "After", currency, 2)
        queries.handle(new GetSettlement(other)) == new SettlementView(other, "Other", currency, 1)

        when:
        queries.handle(new GetSettlement(stale))

        then:
        thrown(com.github.monaboiste.fairshare.common.events.MissingStreamException)
    }

    def "duplicate delivery is ignored and a gap is rejected"() {
        given:
        def projector = new SettlementProjector()
        def opened = new com.github.monaboiste.fairshare.common.events.EventEnvelope(UUID.randomUUID(), ID, 1, NOW,
            new SettlementOpened("First", Monetary.getCurrency("EUR")))
        projector.accept(opened)

        when:
        projector.accept(opened)

        then:
        projector.get(ID).version() == 1

        when:
        projector.accept(new com.github.monaboiste.fairshare.common.events.EventEnvelope(UUID.randomUUID(), ID, 3,
            NOW, new SettlementRenamed("Skipped")))

        then:
        thrown(IllegalStateException)
        projector.get(ID).name() == "First"
    }

    def "concurrent renames on the same Settlement serialize without losing updates"() {
        given:
        def app = application(new InMemoryEventStore<>())
        app.commands().dispatch(new OpenSettlement(ID, "First", Monetary.getCurrency("EUR")))
        def pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        def start = new java.util.concurrent.CountDownLatch(1)

        when:
        def renames = ["Second", "Third"].collect { name ->
            pool.submit({ -> start.await(); app.commands().dispatch(new RenameSettlement(ID, name)) }
                as java.util.concurrent.Callable)
        }
        start.countDown()
        renames*.get()

        then:
        app.queries().handle(new GetSettlement(ID)).version() == 3
        app.queries().handle(new GetSettlementHistory(ID))*.sequence() == [1L, 2L, 3L]

        cleanup:
        pool?.shutdownNow()
    }

    def "repository serializes decisions from separate handlers sharing one repository"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def publisher = new CommittedSettlementEvents()
        def repository = new DefaultSourcedSettlementRepository(store, publisher)
        def first = new RenameSettlementHandler(repository, Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID)
        def second = new RenameSettlementHandler(repository, Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID)
        def opener = new OpenSettlementHandler(repository, Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID)
        opener.handle(new OpenSettlement(ID, "First", Monetary.getCurrency("EUR")))
        def pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        def start = new java.util.concurrent.CountDownLatch(1)

        when:
        def renames = [[first, "Second"], [second, "Third"]].collect { pair ->
            pool.submit({ -> start.await(); pair[0].handle(new RenameSettlement(ID, pair[1])) }
                as java.util.concurrent.Callable)
        }
        start.countDown()
        renames*.get()

        then:
        store.load(ID)*.sequence() == [1L, 2L, 3L]

        cleanup:
        pool?.shutdownNow()
    }

    private static SettlementApplication application(store) {
        new SettlementApplication(store, Clock.fixed(NOW, ZoneOffset.UTC), UUID::randomUUID)
    }
}
