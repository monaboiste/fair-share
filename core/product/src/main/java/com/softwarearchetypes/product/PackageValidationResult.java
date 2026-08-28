package com.softwarearchetypes.product;

import java.util.List;

/** The outcome and errors from validating a package selection. */
public record PackageValidationResult(boolean valid, List<String> errors) {

    public static PackageValidationResult success() {
        return new PackageValidationResult(true, List.of());
    }

    public static PackageValidationResult failure(String error) {
        return new PackageValidationResult(false, List.of(error));
    }

    public static PackageValidationResult failure(List<String> errors) {
        return new PackageValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }
}
