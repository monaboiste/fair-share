package com.github.monaboiste.fairshare.settlement.domain.model;

import com.github.monaboiste.fairshare.settlement.domain.ParticipantName;

final class Participant {
    private final String addedName;
    private String name;
    private ParticipantStatus status;

    Participant(String addedName) {
        this.addedName = addedName;
        name = addedName;
        status = ParticipantStatus.ACTIVE;
    }

    boolean isAddedAs(ParticipantName name) {
        return addedName.equals(name.value());
    }

    boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    boolean isNamed(ParticipantName name) {
        return this.name.equals(name.value());
    }

    void rename(String name) {
        if (!isActive()) {
            throw new IllegalStateException("Participant missing for rename");
        }
        this.name = name;
    }

    void remove() {
        if (!isActive()) {
            throw new IllegalStateException("Participant missing for removal");
        }
        status = ParticipantStatus.REMOVED;
    }

    private enum ParticipantStatus {
        ACTIVE,
        REMOVED
    }
}
