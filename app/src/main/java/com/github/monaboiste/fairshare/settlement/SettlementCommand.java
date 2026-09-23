package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.Command;

public sealed interface SettlementCommand<R> extends Command<R> permits OpenSettlement, RenameSettlement {}
