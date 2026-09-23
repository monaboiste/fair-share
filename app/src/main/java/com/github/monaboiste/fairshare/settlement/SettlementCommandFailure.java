package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.CommandFailure;

public sealed interface SettlementCommandFailure extends CommandFailure permits IdentifierConflict {}
