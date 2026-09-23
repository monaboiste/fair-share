package com.github.monaboiste.fairshare.common;

public interface CommandDispatcher {
    <R> R dispatch(Command<R> command);
}
