package com.softwarearchetypes.pricing;

import java.util.UUID;

public record ComponentVersionId(UUID value) {

    public static ComponentVersionId generate() {
        return new ComponentVersionId(UUID.randomUUID());
    }
}
