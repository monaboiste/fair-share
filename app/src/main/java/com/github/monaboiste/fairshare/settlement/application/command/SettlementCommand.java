package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.commands.Command;
import com.github.monaboiste.fairshare.common.commands.CommandFailure;

public sealed interface SettlementCommand<F extends CommandFailure, S> extends Command<F, S>
        permits OpenSettlement, RenameSettlement {}
