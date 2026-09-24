package com.github.monaboiste.fairshare.common.domain;

public interface WriteRepository<T> {
    void save(T aggregate);
}
