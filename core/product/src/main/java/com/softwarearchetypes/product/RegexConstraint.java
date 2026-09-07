package com.softwarearchetypes.product;

import java.util.regex.Pattern;

/** Restricts text to values matching a regular expression. */
record RegexConstraint(Pattern compiledPattern) implements FeatureValueConstraint {

    RegexConstraint(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Pattern must be defined");
        }
        this(Pattern.compile(pattern));
    }

    static FeatureValueConstraint of(String pattern) {
        return new RegexConstraint(pattern);
    }

    String pattern() {
        return compiledPattern.pattern();
    }

    @Override
    public FeatureValueType valueType() {
        return FeatureValueType.TEXT;
    }

    @Override
    public String type() {
        return "REGEX";
    }

    @Override
    public boolean isValid(Object value) {
        return value instanceof String string && compiledPattern.matcher(string).matches();
    }

    @Override
    public String desc() {
        return "text matching pattern: " + pattern();
    }
}
