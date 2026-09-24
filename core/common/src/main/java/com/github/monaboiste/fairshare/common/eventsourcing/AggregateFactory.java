package com.github.monaboiste.fairshare.common.eventsourcing;

@FunctionalInterface
public interface AggregateFactory<ID, A> {
    A create(ID id);
}
