package com.github.monaboiste.fairshare.common.domain;

import java.util.Optional;

@SuppressWarnings("squid:S119")
public interface ReadRepository<ID, T> {
    Optional<T> findById(ID id);
}
