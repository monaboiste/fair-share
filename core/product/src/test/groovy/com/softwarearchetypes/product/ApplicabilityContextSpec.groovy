package com.softwarearchetypes.product

import spock.lang.Specification

class ApplicabilityContextSpec extends Specification {

    def "store parameters immutably"() {
        given:
        Map<String, String> parameters = new HashMap<>(Map.of("channel", "mobile"))

        when:
        ApplicabilityContext context = ApplicabilityContext.of(parameters)
        parameters.put("channel", "web")

        then:
        context.get("channel").orElseThrow() == "mobile"
    }

    def "return value for known key"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "mobile"))

        then:
        context.get("channel").orElseThrow() == "mobile"
        !context.get("missing").isPresent()
    }

    def "return default for unknown key"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "mobile"))

        then:
        context.getOrDefault("missing", "default") == "default"
    }

    def "report whether a key is present"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "mobile"))

        then:
        context.has("channel")
        !context.has("missing")
    }

    def "expose parameters as immutable map"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "mobile"))

        then:
        context.asMap() == Map.of("channel", "mobile")
    }

    def "format as string"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", "mobile"))

        then:
        context.toString() == "ApplicabilityContext{channel=mobile}"
    }

    def "treat null parameters as empty"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(null)

        then:
        context.asMap().isEmpty()
    }
}
