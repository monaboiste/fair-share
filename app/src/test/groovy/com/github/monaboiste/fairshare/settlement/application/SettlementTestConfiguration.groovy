package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.commands.RegisteredCommandDispatcher
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.common.queries.RegisteredQueryDispatcher
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.command.SettlementCommand
import com.github.monaboiste.fairshare.settlement.application.command.handler.AddParticipantHandler
import com.github.monaboiste.fairshare.settlement.application.command.handler.OpenSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.command.handler.RemoveParticipantHandler
import com.github.monaboiste.fairshare.settlement.application.command.handler.RenameParticipantHandler
import com.github.monaboiste.fairshare.settlement.application.command.handler.RenameSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.SettlementQuery
import com.github.monaboiste.fairshare.settlement.application.query.handler.GetSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.query.handler.GetSettlementHistoryHandler
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.CurrencyUnit
import javax.money.Monetary

class SettlementTestConfiguration {
    public static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC)
    public static final CurrencyUnit EUR = Monetary.getCurrency("EUR")
    public static final CurrencyUnit USD = Monetary.getCurrency("USD")
    public static final SettlementId UNKNOWN_ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    final InMemoryEventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
    final SettlementProjector projector = new SettlementProjector()
    final SettlementRepository repository = new EventSourcedSettlementRepository(store, CLOCK)
    final OpenSettlementHandler openHandler = new OpenSettlementHandler(repository, CLOCK)
    final RenameSettlementHandler renameHandler = new RenameSettlementHandler(repository)
    final AddParticipantHandler addHandler = new AddParticipantHandler(repository)
    final RenameParticipantHandler renameParticipantHandler = new RenameParticipantHandler(repository)
    final RemoveParticipantHandler removeParticipantHandler = new RemoveParticipantHandler(repository)
    final GetSettlementHandler viewHandler = new GetSettlementHandler(projector)
    final GetSettlementHistoryHandler historyHandler = new GetSettlementHistoryHandler(store)
    final RegisteredCommandDispatcher commands = RegisteredCommandDispatcher.builder()
        .register(OpenSettlement, openHandler)
        .register(RenameSettlement, renameHandler)
        .register(AddParticipant, addHandler)
        .register(RenameParticipant, renameParticipantHandler)
        .register(RemoveParticipant, removeParticipantHandler)
        .requireHandlersFor(SettlementCommand).build()
    final RegisteredQueryDispatcher queries = RegisteredQueryDispatcher.builder()
        .register(GetSettlement, viewHandler)
        .register(GetSettlementHistory, historyHandler)
        .requireHandlersFor(SettlementQuery).build()

    SettlementTestConfiguration() {
        store.subscribe(projector)
    }

    SettlementId openSettlement(String name, CurrencyUnit currency = EUR) {
        openHandler.handle(new OpenSettlement(new SettlementName(name), currency)).getSuccess().streamId()
    }
}
