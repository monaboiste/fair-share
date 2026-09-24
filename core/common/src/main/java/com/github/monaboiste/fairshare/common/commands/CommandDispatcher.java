package com.github.monaboiste.fairshare.common.commands;

import com.github.monaboiste.fairshare.common.Result;

public interface CommandDispatcher {
    <F extends CommandFailure, S> Result<F, S> dispatch(Command<F, S> command);
}
