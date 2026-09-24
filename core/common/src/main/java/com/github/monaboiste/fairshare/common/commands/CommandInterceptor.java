package com.github.monaboiste.fairshare.common.commands;

import com.github.monaboiste.fairshare.common.Result;

public interface CommandInterceptor {
    <F extends CommandFailure, S> Result<F, S> intercept(Command<F, S> command, Proceed proceed);

    interface Proceed {
        <F extends CommandFailure, S> Result<F, S> handle(Command<F, S> command);
    }
}
