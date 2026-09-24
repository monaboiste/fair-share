package com.github.monaboiste.fairshare.common.commands;

public interface CommandDispatcher {
    <R> R dispatch(Command<R> command);
}
