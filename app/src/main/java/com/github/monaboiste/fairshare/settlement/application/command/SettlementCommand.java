package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.commands.Command;

public sealed interface SettlementCommand<R> extends Command<R> permits OpenSettlement, RenameSettlement {}
