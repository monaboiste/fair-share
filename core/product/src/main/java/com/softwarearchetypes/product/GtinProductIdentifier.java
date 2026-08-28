package com.softwarearchetypes.product;

import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * GTIN (Global Trade Item Number) identifies retail products.
 *
 * <p>Supported formats:
 *
 * <ul>
 *   <li>GTIN-8 (EAN-8)
 *   <li>GTIN-12 (UPC-A)
 *   <li>GTIN-13 (EAN-13)
 *   <li>GTIN-14
 * </ul>
 *
 * <p>Each format contains a company prefix, item reference, and check digit.
 */
record GtinProductIdentifier(String value) implements ProductIdentifier {

    private static final Pattern SEPARATORS = Pattern.compile("[-\\s]+");
    private static final Pattern VALID_FORMAT =
            Pattern.compile("[0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14}"); // NOSONAR: GTIN requires ASCII digits

    GtinProductIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GTIN cannot be null or blank");
        }

        value = SEPARATORS.matcher(value).replaceAll("");

        if (!VALID_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("GTIN must be 8, 12, 13, or 14 digits");
        }

        if (!hasValidCheckDigit(value)) {
            throw new IllegalArgumentException("Invalid GTIN check digit");
        }
    }

    static GtinProductIdentifier of(String value) {
        return new GtinProductIdentifier(value);
    }

    @Override
    public String type() {
        return "GTIN-" + value.length();
    }

    @Override
    public @NonNull String toString() {
        return value;
    }

    /**
     * Validates the check digit using the GTIN modulo-10 algorithm.
     *
     * @param gtin normalized GTIN containing only digits
     * @return {@code true} when the check digit is valid
     */
    private static boolean hasValidCheckDigit(String gtin) {
        int length = gtin.length();
        int sum = 0;

        for (int i = 0; i < length - 1; i++) {
            int digit = gtin.charAt(i) - '0';
            int weight = (length - i) % 2 == 0 ? 3 : 1;
            sum += digit * weight;
        }

        int checkDigit = gtin.charAt(length - 1) - '0';
        int expectedCheckDigit = (10 - sum % 10) % 10;

        return checkDigit == expectedCheckDigit;
    }
}
