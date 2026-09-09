package com.softwarearchetypes.product;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * VIN (Vehicle Identification Number) identifies a motor vehicle.
 *
 * <p>A VIN consists of 17 uppercase letters and digits, excluding {@code I}, {@code O}, and {@code Q}. Its sections
 * are:
 *
 * <ul>
 *   <li>3-character WMI (World Manufacturer Identifier);
 *   <li>6-character VDS (Vehicle Descriptor Section), including the check-digit position;
 *   <li>8-character VIS (Vehicle Identifier Section), including the model year, plant code, and serial number.
 * </ul>
 *
 * <p>Examples include {@code 5YJ3E1EA1JF000001} and {@code 1HGBH41JXMN109186}.
 */
record VinSerialNumber(String value) implements SerialNumber {

    private static final int REQUIRED_LENGTH = 17;

    private static final Pattern SEPARATORS = Pattern.compile("[-\\s]+");
    private static final Pattern VALID_CHARACTERS = Pattern.compile("[A-HJ-NPR-Z\\d]+");

    VinSerialNumber {
        if (value.isBlank()) {
            throw new IllegalArgumentException("VIN cannot be null or blank");
        }

        value = SEPARATORS.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);

        if (value.length() != REQUIRED_LENGTH) {
            throw new IllegalArgumentException("VIN must be exactly 17 characters");
        }

        if (!VALID_CHARACTERS.matcher(value).matches()) {
            throw new IllegalArgumentException("VIN must contain only letters and digits, excluding I, O, and Q");
        }
    }

    static VinSerialNumber of(String value) {
        return new VinSerialNumber(value);
    }

    @Override
    public String type() {
        return "VIN";
    }

    @Override
    public String toString() {
        return value;
    }
}
