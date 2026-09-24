package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.commands.CommandFailure;

public sealed interface SettlementCommandFailure extends CommandFailure
        permits IdentifierConflict, SettlementNotFound {}
