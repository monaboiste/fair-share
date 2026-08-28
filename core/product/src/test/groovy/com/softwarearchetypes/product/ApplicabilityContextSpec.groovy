package com.softwarearchetypes.product

import spock.lang.Specification

class ApplicabilityContextSpec extends Specification {

    def "should expose immutable applicability parameters"() {
        given:
        Map<String, String> parameters = new HashMap<>(Map.of("channel", "mobile"))

        when:
        ApplicabilityContext context = ApplicabilityContext.of(parameters)
        parameters.put("channel", "web")

        then:
        context.get("channel").orElseThrow() == "mobile"
        context.getOrDefault("missing", "default") == "default"
        context.has("channel")
        !context.has("missing")
        context.asMap() == Map.of("channel", "mobile")
        context.toString() == "ApplicabilityContext{channel=mobile}"
    }

    def "should treat null parameters as empty"() {
        when:
        ApplicabilityContext context = ApplicabilityContext.of(null)

        then:
        context.asMap().isEmpty()
    }
}
