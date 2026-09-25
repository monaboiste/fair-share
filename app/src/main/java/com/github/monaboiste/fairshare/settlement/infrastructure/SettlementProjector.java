package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.common.events.AllEventsReader;
import com.github.monaboiste.fairshare.common.events.CommittedEventsListener;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.settlement.application.query.ParticipantView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementViews;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved;
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
            Projection projection =
                    staged.containsKey(event.streamId()) ? staged.get(event.streamId()) : current.get(event.streamId());
            SettlementView previous = projection == null ? null : projection.view();
            Set<ParticipantId> retired = projection == null ? Set.of() : projection.retiredParticipantIds();
            long version = previous == null ? 0 : previous.version();
            if (event.sequence() <= version) {
                continue;
            }
            if (event.sequence() != version + 1) {
                throw new IllegalStateException(
                        "Gap in Settlement %s at %s".formatted(event.streamId(), event.sequence()));
            }
            SettlementView next =
                    switch (event.payload()) {
                        case SettlementOpened(var name, var currency) -> {
                            if (previous != null) {
                                throw new IllegalStateException("Settlement already projected");
                            }
                            yield new SettlementView(event.streamId(), name, currency, event.sequence(), List.of());
                        }
                        case SettlementRenamed(var name) -> {
                            if (previous == null) {
                                throw new IllegalStateException("Settlement opening missing");
                            }
                            yield new SettlementView(
                                    event.streamId(),
                                    name,
                                    previous.currency(),
                                    event.sequence(),
                                    previous.participants());
                        }
                        case ParticipantAdded(var participantId, var name) -> {
                            if (previous == null
                                    || retired.contains(participantId)
                                    || previous.participants().stream().anyMatch(p -> p.id().equals(participantId))) {
                                throw new IllegalStateException("Invalid Participant addition");
                            }
                            List<ParticipantView> participants = new ArrayList<>(previous.participants());
                            participants.add(new ParticipantView(participantId, name));
                            yield new SettlementView(
                                    event.streamId(),
                                    previous.name(),
                                    previous.currency(),
                                    event.sequence(),
                                    List.copyOf(participants));
                        }
                        case ParticipantRenamed(var participantId, var name) -> {
                            if (previous == null
                                    || previous.participants().stream().noneMatch(p -> p.id().equals(participantId))) {
                                throw new IllegalStateException("Participant missing for rename");
                            }
                            List<ParticipantView> participants = previous.participants().stream()
                                    .map(p ->
                                            p.id().equals(participantId) ? new ParticipantView(participantId, name) : p)
                                    .toList();
                            yield new SettlementView(
                                    event.streamId(),
                                    previous.name(),
                                    previous.currency(),
                                    event.sequence(),
                                    participants);
                        }
                        case ParticipantRemoved(var participantId) -> {
                            if (previous == null
                                    || previous.participants().stream().noneMatch(p -> p.id().equals(participantId))) {
                                throw new IllegalStateException("Participant missing for removal");
                            }
                            List<ParticipantView> participants = previous.participants().stream()
                                    .filter(p -> !p.id().equals(participantId))
                                    .toList();
                            yield new SettlementView(
                                    event.streamId(),
                                    previous.name(),
                                    previous.currency(),
                                    event.sequence(),
                                    participants);
                        }
                    };
            Set<ParticipantId> nextRetired = retired;
            if (event.payload() instanceof ParticipantRemoved(var removedId)) {
                Set<ParticipantId> withRemoved = new HashSet<>(retired);
                withRemoved.add(removedId);
                nextRetired = Set.copyOf(withRemoved);
            }
            staged.put(event.streamId(), new Projection(next, nextRetired));
        }
        current.putAll(staged);
        return current;
    }

    @Override
    public synchronized Optional<SettlementView> findById(SettlementId id) {
        return Optional.ofNullable(projections.get(id)).map(Projection::view);
    }

    private record Projection(SettlementView view, Set<ParticipantId> retiredParticipantIds) {}
}
