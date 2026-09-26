package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.commands.Command;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;

public sealed interface SettlementCommand
        extends Command<SettlementRejection, CommitResult<SettlementId, SettlementEvent>>
        permits OpenSettlement, RenameSettlement, AddParticipant, RenameParticipant, RemoveParticipant, RecordExpense {}
