package com.softwarearchetypes.product;

import java.util.regex.Pattern;

/**
 * ISBN-10 (International Standard Book Number) identifies a book or publication.
 *
 * <p>An ISBN-10 consists of:
 *
 * <ul>
 *   <li>a group identifier;
 *   <li>a publisher identifier;
 *   <li>a title identifier;
 *   <li>a check character containing a digit or {@code X}.
 * </ul>
 *
 * <p>For example, {@code 0-201-77060-1} is stored as {@code 0201770601}.
 */
record Isbn10ProductIdentifier(String value) implements ProductIdentifier {

    private static final Pattern SEPARATORS = Pattern.compile("[-\\s]+");
    private static final Pattern VALID_FORMAT = Pattern.compile("\\d{9}[\\dX]");

    Isbn10ProductIdentifier {
        if (value.isBlank()) {
            throw new IllegalArgumentException("ISBN cannot be null or blank");
        }

        value = SEPARATORS.matcher(value).replaceAll("").replace('x', 'X');

        if (!VALID_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("ISBN-10 must contain 9 digits followed by a digit or X");
        }

        if (!hasValidCheckDigit(value)) {
            throw new IllegalArgumentException("Invalid ISBN-10 check digit");
        }
    }

    static Isbn10ProductIdentifier of(String value) {
        return new Isbn10ProductIdentifier(value);
    }

    @Override
    public String type() {
        return "ISBN-10";
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Validates the check character using the ISBN-10 modulo-11 algorithm.
     *
     * @param isbn normalized ISBN-10
     * @return {@code true} if the check character is valid; otherwise {@code false}
     */
    private static boolean hasValidCheckDigit(String isbn) {
        int sum = 0;

        for (int i = 0; i < 9; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += digit * (10 - i);
        }

        char checkCharacter = isbn.charAt(9);
        int checkValue = checkCharacter == 'X' ? 10 : checkCharacter - '0';

        return (sum + checkValue) % 11 == 0;
    }
}
