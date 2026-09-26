package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.common.events.AllEventsReader;
import com.github.monaboiste.fairshare.common.events.CommittedEventsListener;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.netting.Obligation;
import com.github.monaboiste.fairshare.netting.Obligations;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.application.query.ExpenseView;
import com.github.monaboiste.fairshare.settlement.application.query.ParticipantView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementViews;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.Share;
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class SettlementProjector
        implements SettlementViews, CommittedEventsListener<SettlementId, SettlementEvent> {
    private Map<SettlementId, Projection> projections = new HashMap<>();

    public synchronized void rebuild(AllEventsReader<SettlementId, SettlementEvent> events) {
        projections = project(new HashMap<>(), events.readAll(0));
    }

    @Override
    public synchronized void accept(List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        project(projections, events);
    }

    private Map<SettlementId, Projection> project(
            Map<SettlementId, Projection> current, List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        Map<SettlementId, Projection> staged = new HashMap<>();
        for (EventEnvelope<SettlementId, SettlementEvent> event : events) {
            SettlementId id = event.streamId();
            Projection stagedProjection = staged.get(id);
            Projection projection = stagedProjection != null ? stagedProjection : current.get(id);
            if (alreadyProjected(event, projection)) {
                continue;
            }
            validateSequence(event, projection);
            staged.put(id, apply(projection, event));
        }
        current.putAll(staged);
        return current;
    }

    private static boolean alreadyProjected(
            EventEnvelope<SettlementId, SettlementEvent> event, @Nullable Projection projection) {
        return projection != null && event.sequence() <= projection.view().version();
    }

    private static void validateSequence(
            EventEnvelope<SettlementId, SettlementEvent> event, @Nullable Projection projection) {
        long version = projection == null ? 0 : projection.view().version();
        if (event.sequence() != version + 1) {
            throw new IllegalStateException("Gap in Settlement %s at %s".formatted(event.streamId(), event.sequence()));
        }
    }

    private static Projection apply(
            @Nullable Projection projection, EventEnvelope<SettlementId, SettlementEvent> event) {
        SettlementView previous = projection == null ? null : projection.view();
        Set<ParticipantId> retired = projection == null ? Set.of() : projection.retiredParticipantIds();
        return switch (event.payload()) {
            case SettlementOpened(var name, var currency) -> {
                if (previous != null) {
                    throw new IllegalStateException("Settlement already projected");
                }
                yield new Projection(
                        new SettlementView(
                                event.streamId(),
                                name,
                                currency,
                                event.sequence(),
                                List.of(),
                                List.of(),
                                List.of(),
                                Map.of()),
                        retired);
            }
            case ExpenseRecorded recorded -> recordExpense(requireSettlement(previous), retired, event, recorded);
            case SettlementRenamed(var name) -> {
                SettlementView view = requireSettlement(previous);
                yield new Projection(
                        new SettlementView(
                                event.streamId(),
                                name,
                                view.currency(),
                                event.sequence(),
                                view.participants(),
                                view.expenses(),
                                view.obligations(),
                                view.balances()),
                        retired);
            }
            case ParticipantAdded(var participantId, var name) ->
                addParticipant(requireSettlement(previous), retired, event, new ParticipantView(participantId, name));
            case ParticipantRenamed(var participantId, var name) ->
                renameParticipant(
                        requireSettlement(previous), retired, event, new ParticipantView(participantId, name));
            case ParticipantRemoved(var participantId) ->
                removeParticipant(requireSettlement(previous), retired, event, participantId);
        };
    }

    private static Projection addParticipant(
            SettlementView previous,
            Set<ParticipantId> retired,
            EventEnvelope<SettlementId, SettlementEvent> event,
            ParticipantView participant) {
        if (retired.contains(participant.id()) || containsParticipant(previous, participant.id())) {
            throw new IllegalStateException("Invalid Participant addition");
        }
        List<ParticipantView> participants = new ArrayList<>(previous.participants());
        participants.add(participant);
        return new Projection(updated(previous, event, List.copyOf(participants)), retired);
    }

    private static Projection renameParticipant(
            SettlementView previous,
            Set<ParticipantId> retired,
            EventEnvelope<SettlementId, SettlementEvent> event,
            ParticipantView renamedParticipant) {
        requireParticipant(previous, renamedParticipant.id(), "Participant missing for rename");
        List<ParticipantView> participants = previous.participants().stream()
                .map(participant -> participant.id().equals(renamedParticipant.id()) ? renamedParticipant : participant)
                .toList();
        return new Projection(updated(previous, event, participants), retired);
    }

    private static Projection removeParticipant(
            SettlementView previous,
            Set<ParticipantId> retired,
            EventEnvelope<SettlementId, SettlementEvent> event,
            ParticipantId participantId) {
        requireParticipant(previous, participantId, "Participant missing for removal");
        if (previous.expenses().stream()
                .anyMatch(expense -> expense.payer().equals(participantId)
                        || expense.allocation().recipients().contains(participantId))) {
            throw new IllegalStateException("Participant referenced by Expense");
        }
        List<ParticipantView> participants = previous.participants().stream()
                .filter(participant -> !participant.id().equals(participantId))
                .toList();
        Set<ParticipantId> nextRetired = new HashSet<>(retired);
        nextRetired.add(participantId);
        return new Projection(updated(previous, event, participants), Set.copyOf(nextRetired));
    }

    private static SettlementView updated(
            SettlementView previous,
            EventEnvelope<SettlementId, SettlementEvent> event,
            List<ParticipantView> participants) {
        return updated(previous, event, participants, previous.expenses(), previous.obligations());
    }

    private static SettlementView updated(
            SettlementView previous,
            EventEnvelope<SettlementId, SettlementEvent> event,
            List<ParticipantView> participants,
            List<ExpenseView> expenses,
            List<Obligation<ParticipantId>> obligations) {
        Set<ParticipantId> roster = new HashSet<>();
        participants.forEach(participant -> roster.add(participant.id()));
        Map<ParticipantId, Money> computed =
                Obligations.of(roster, obligations, previous.currency()).signedBalances();
        Map<ParticipantId, Money> ordered = new LinkedHashMap<>();
        participants.forEach(participant -> ordered.put(participant.id(), computed.get(participant.id())));
        return new SettlementView(
                event.streamId(),
                previous.name(),
                previous.currency(),
                event.sequence(),
                participants,
                expenses,
                obligations,
                Collections.unmodifiableMap(ordered));
    }

    private static Projection recordExpense(
            SettlementView previous,
            Set<ParticipantId> retired,
            EventEnvelope<SettlementId, SettlementEvent> event,
            ExpenseRecorded recorded) {
        if (previous.expenses().stream().anyMatch(expense -> expense.id().equals(recorded.expenseId()))) {
            throw new IllegalStateException("Duplicate Expense");
        }
        requireParticipant(previous, recorded.payer(), "Expense payer missing");
        recorded.allocation()
                .recipients()
                .forEach(recipient -> requireParticipant(previous, recipient, "Expense recipient missing"));
        List<ExpenseView> expenses = new ArrayList<>(previous.expenses());
        expenses.add(new ExpenseView(
                recorded.expenseId(),
                recorded.description(),
                recorded.incurredOn(),
                recorded.payer(),
                recorded.originalAmount(),
                recorded.allocation(),
                recorded.componentVersionId(),
                recorded.exchangeRate(),
                recorded.valuation(),
                recorded.shares(),
                ExpenseView.Status.ACTIVE));
        List<Obligation<ParticipantId>> obligations = new ArrayList<>(previous.obligations());
        for (Share share : recorded.shares()) {
            if (!share.participantId().equals(recorded.payer())) {
                obligations.add(Obligation.of(share.participantId(), recorded.payer(), share.amount()));
            }
        }
        return new Projection(
                updated(previous, event, previous.participants(), List.copyOf(expenses), List.copyOf(obligations)),
                retired);
    }

    private static SettlementView requireSettlement(@Nullable SettlementView view) {
        if (view == null) {
            throw new IllegalStateException("Settlement opening missing");
        }
        return view;
    }

    private static void requireParticipant(SettlementView view, ParticipantId participantId, String message) {
        if (!containsParticipant(view, participantId)) {
            throw new IllegalStateException(message);
        }
    }

    private static boolean containsParticipant(SettlementView view, ParticipantId participantId) {
        return view.participants().stream()
                .anyMatch(participant -> participant.id().equals(participantId));
    }

    @Override
    public synchronized Optional<SettlementView> findById(SettlementId id) {
        return Optional.ofNullable(projections.get(id)).map(Projection::view);
    }

    private record Projection(SettlementView view, Set<ParticipantId> retiredParticipantIds) {}
}
