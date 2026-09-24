package com.github.monaboiste.fairshare.common.commands;

import com.github.monaboiste.fairshare.common.Result;

@FunctionalInterface
public interface CommandHandler<C extends Command<F, S>, F extends CommandFailure, S> {
    Result<F, S> handle(C command);
}
