package com.github.monaboiste.fairshare.settlement.domain.aggregate;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateFactory;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateRoot;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public final class Settlement extends AggregateRoot<SettlementId, SettlementEvent> {
    private final SettlementId id;
    private @Nullable String name;
    private @Nullable CurrencyUnit currency;
    private final Map<ParticipantId, Participant> participants = new HashMap<>();

    private Settlement(SettlementId id, Clock clock) {
        super(clock);
        this.id = id;
    }

    public static AggregateFactory<SettlementId, Settlement> factory(Clock clock) {
        return id -> new Settlement(id, clock);
    }

    public static Settlement open(SettlementName name, CurrencyUnit currency, Clock clock) {
        Settlement settlement = new Settlement(SettlementId.random(), clock);
        settlement.register(new SettlementOpened(name.value(), currency));
        return settlement;
    }

    public void rename(SettlementName name) {
        if (!name.value().equals(this.name)) {
            register(new SettlementRenamed(name.value()));
        }
    }

    public Result<ParticipantIdentifierConflict, ParticipantId> addParticipant(
            ParticipantId participantId, ParticipantName name) {
        Participant participant = participants.get(participantId);
        if (participant != null) {
            return participant.isAddedAs(name)
                    ? Result.success(participantId)
                    : Result.failure(new ParticipantIdentifierConflict(id, participantId));
        }
        register(new ParticipantAdded(participantId, name.value()));
        return Result.success(participantId);
    }

    public Result<ParticipantNotFound, ParticipantId> renameParticipant(
            ParticipantId participantId, ParticipantName name) {
        Participant participant = participants.get(participantId);
        if (participant == null || !participant.isActive()) {
            return Result.failure(new ParticipantNotFound(id, participantId));
        }
        if (!participant.isNamed(name)) {
            register(new ParticipantRenamed(participantId, name.value()));
        }
        return Result.success(participantId);
    }

    public Result<ParticipantNotFound, ParticipantId> removeParticipant(ParticipantId participantId) {
        Participant participant = participants.get(participantId);
        if (participant == null || !participant.isActive()) {
            return Result.failure(new ParticipantNotFound(id, participantId));
        }
        register(new ParticipantRemoved(participantId));
        return Result.success(participantId);
    }

    @Override
    public SettlementId id() {
        return id;
    }

    @Override
    protected void apply(SettlementEvent event) {
        switch (event) {
            case SettlementOpened(var openedName, var openedCurrency) -> {
                if (currency != null) {
                    throw new IllegalStateException("Settlement already opened");
                }
                name = openedName;
                currency = openedCurrency;
            }
            case ParticipantAdded(var participantId, var participantName) -> {
                if (currency == null || participants.containsKey(participantId)) {
                    throw new IllegalStateException("Invalid Participant addition");
                }
                participants.put(participantId, new Participant(participantId, participantName));
            }
            case ParticipantRenamed(var participantId, var participantName) -> {
                if (currency == null || !participants.containsKey(participantId)) {
                    throw new IllegalStateException("Participant missing for rename");
                }
                participants.get(participantId).rename(participantName);
            }
            case ParticipantRemoved(var participantId) -> {
                if (currency == null || !participants.containsKey(participantId)) {
                    throw new IllegalStateException("Participant missing for removal");
                }
                participants.get(participantId).remove();
            }
            case SettlementRenamed(var newName) -> {
                if (currency == null) {
                    throw new IllegalStateException("Settlement opening missing");
                }
                name = newName;
            }
        }
    }
}
