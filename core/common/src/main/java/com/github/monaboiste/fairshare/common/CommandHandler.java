package com.github.monaboiste.fairshare.common;

public interface CommandHandler<C extends Command<R>, R> {
    Class<C> commandType();

    R handle(C command);
}
