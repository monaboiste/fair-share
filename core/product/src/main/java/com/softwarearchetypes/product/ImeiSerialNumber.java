package com.softwarearchetypes.product;

import java.util.regex.Pattern;

/**
 * IMEI (International Mobile Equipment Identity) identifies mobile equipment.
 *
 * <p>An IMEI consists of 15 digits:
 *
 * <ul>
 *   <li>8-digit TAC (Type Allocation Code), identifying the device type;
 *   <li>6-digit serial number;
 *   <li>1 check digit calculated using the Luhn algorithm.
 * </ul>
 *
 * <p>For example: {@code 490154203237518}.
 */
record ImeiSerialNumber(String value) implements SerialNumber {

    private static final Pattern SEPARATORS = Pattern.compile("[-\\s]+");
    private static final Pattern VALID_FORMAT = Pattern.compile("\\d{15}");

    ImeiSerialNumber {
        if (value.isBlank()) {
            throw new IllegalArgumentException("IMEI cannot be null or blank");
        }

        value = SEPARATORS.matcher(value).replaceAll("");

        if (!VALID_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("IMEI must be exactly 15 digits");
        }

        if (!hasValidChecksum(value)) {
            throw new IllegalArgumentException("Invalid IMEI check digit");
        }
    }

    static ImeiSerialNumber of(String value) {
        return new ImeiSerialNumber(value);
    }

    @Override
    public String type() {
        return "IMEI";
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Validates the IMEI check digit using the Luhn algorithm.
     *
     * @param imei normalized 15-digit IMEI
     * @return {@code true} if the checksum is valid; otherwise {@code false}
     */
    private static boolean hasValidChecksum(String imei) {
        int sum = 0;
        boolean doubleDigit = false;

        for (int i = imei.length() - 1; i >= 0; i--) {
            int digit = imei.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }
}
