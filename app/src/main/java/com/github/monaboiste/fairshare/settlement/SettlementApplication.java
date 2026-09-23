package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.CommandDispatcher;
import com.github.monaboiste.fairshare.common.RegisteredCommandDispatcher;
import com.github.monaboiste.fairshare.common.events.EventStore;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class SettlementApplication {
    private final CommandDispatcher commands;
    private final SettlementQueryHandler queries;

    public SettlementApplication(
            EventStore<SettlementId, SettlementEvent> store, Clock clock, Supplier<UUID> eventIds) {
        SettlementProjector projector = new SettlementProjector();
        projector.rebuild(store);
        CommittedSettlementEvents publisher = new CommittedSettlementEvents();
        publisher.subscribe(projector::accept);
        var repository = new DefaultSourcedSettlementRepository(store, publisher);
        commands = new RegisteredCommandDispatcher(List.of(
                new OpenSettlementHandler(repository, clock, eventIds),
                new RenameSettlementHandler(repository, clock, eventIds)));
        queries = new SettlementQueryHandler(store, projector);
    }

    public CommandDispatcher commands() {
        return commands;
    }

    public SettlementQueryHandler queries() {
        return queries;
    }
}
