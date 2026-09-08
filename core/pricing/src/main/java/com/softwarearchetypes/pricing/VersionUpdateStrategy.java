package com.softwarearchetypes.pricing;

import java.util.List;

/** Defines how validity conflicts are validated when adding component versions. */
public enum VersionUpdateStrategy {

    /** Rejects an identical validity period while allowing other overlaps. */
    REJECT_IDENTICAL {
        @Override
        void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity) {
            for (ComponentVersion version : existingVersions) {
                if (version.validity().equals(newValidity)) {
                    throw new IllegalArgumentException(("Version with identical validity period already exists: %s. "
                                    + "Use different validFrom/validTo to create temporal overlaps.")
                            .formatted(newValidity));
                }
            }
        }
    },

    /** Rejects any overlap with an existing validity period. */
    REJECT_OVERLAPPING {
        @Override
        void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity) {
            for (ComponentVersion version : existingVersions) {
                if (version.validity().overlaps(newValidity)) {
                    throw new IllegalArgumentException(("New validity period %s overlaps with existing period %s. "
                                    + "Use REJECT_IDENTICAL strategy if overlaps are intentional.")
                            .formatted(newValidity, version.validity()));
                }
            }
        }
    },

    /** Allows duplicate and overlapping validity periods without validation. */
    ALLOW_ALL {
        @Override
        void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity) {
            /* don't validate */
        }
    };

    /**
     * Validates that adding a new version with given validity is allowed.
     *
     * @param existingVersions current versions in the component
     * @param newValidity validity period of version being added
     * @throws IllegalArgumentException when validation fails, according to this strategy
     */
    abstract void validate(List<? extends ComponentVersion> existingVersions, Validity newValidity);
}
