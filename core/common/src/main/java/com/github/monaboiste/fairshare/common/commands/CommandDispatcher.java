package com.github.monaboiste.fairshare.common.commands;

import com.github.monaboiste.fairshare.common.Result;

public interface CommandDispatcher {
    <F, S> Result<F, S> dispatch(Command<F, S> command);
}
