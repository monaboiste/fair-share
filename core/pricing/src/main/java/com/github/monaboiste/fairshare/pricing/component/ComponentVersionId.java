package com.github.monaboiste.fairshare.pricing.component;

import java.util.UUID;

public record ComponentVersionId(UUID value) {

    public static ComponentVersionId generate() {
        return new ComponentVersionId(UUID.randomUUID());
    }
}
