package com.softwarearchetypes.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Provides a timestamp and normalized attributes for pricing decisions. */
public class PricingContext {

    private final Map<String, String> attributes;
    private final LocalDateTime timestamp;

    private PricingContext(Map<String, String> attributes, LocalDateTime timestamp) {
        this.attributes = Map.copyOf(attributes);
        this.timestamp = timestamp;
    }

    /** Creates a context from parameters, using the current time when no timestamp is present. */
    public static PricingContext from(Parameters parameters) {
        LocalDateTime timestamp = parameters.timestamp().orElseGet(LocalDateTime::now);

        Map<String, String> attributes = new HashMap<>();
        for (String key : parameters.keys()) {
            Object value = parameters.get(key);
            if (value instanceof String s) {
                attributes.put(key, s);
            } else if (value instanceof BigDecimal bd) {
                attributes.put(key, bd.toPlainString());
            } else if (value instanceof Number n) {
                attributes.put(key, n.toString());
            }
        }

        return new PricingContext(attributes, timestamp);
    }

    /** Returns the calculation timestamp. */
    public LocalDateTime timestamp() {
        return timestamp;
    }

    /** Returns the attribute, if present. */
    public Optional<String> get(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public String toString() {
        return "PricingContext{timestamp=" + timestamp + ", attributes=" + attributes + "}";
    }
}
