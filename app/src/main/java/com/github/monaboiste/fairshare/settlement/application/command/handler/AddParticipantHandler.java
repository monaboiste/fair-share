package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository;

/**
 * Adds a caller-identified Participant to an existing Settlement.
 *
 * <p>An identical retry succeeds without an envelope or version change, even after rename or removal. Reusing an id
 * with different original data rejects with {@link ParticipantIdentifierConflict}; an unknown Settlement rejects with
 * {@link SettlementNotFound}. Concurrent commits throw {@link VersionConflictException}; store and post-commit failures
 * propagate as exceptions.
 */
public final class AddParticipantHandler
        implements CommandHandler<AddParticipant, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;

    public AddParticipantHandler(SettlementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(AddParticipant command) {
        Settlement settlement = repository.findById(command.settlementId()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.settlementId()));
        }
        var decision = settlement.addParticipant(command.participantId(), command.name());
        if (decision.failure()) {
            return Result.failure(decision.getFailure());
        }
        return Result.success(repository.save(settlement));
    }
}
