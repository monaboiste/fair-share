package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.common.commands.CommandFailure;

public sealed interface SettlementRejection extends CommandFailure permits IdentifierConflict {}
