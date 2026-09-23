package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.Command;

public sealed interface SettlementCommand extends Command permits OpenSettlement, RenameSettlement {}
