package com.github.monaboiste.fairshare.settlement.domain;

public record ParticipantName(String value) {
    public ParticipantName {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Participant name must not be blank");
        }
    }
}
