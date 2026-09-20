package com.github.monaboiste.fairshare.rules.core;

public interface Modifier<T> {

    T modify(T subject);
}
