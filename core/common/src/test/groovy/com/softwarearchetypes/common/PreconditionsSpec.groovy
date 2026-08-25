package com.softwarearchetypes.common

import spock.lang.Specification

class PreconditionsSpec extends Specification {

    def "checkArgument accepts a true expression"() {
        when:
        Preconditions.checkArgument(true, "This should not be thrown")

        then:
        noExceptionThrown()
    }

    def "checkArgument rejects a false expression"() {
        when:
        Preconditions.checkArgument(false, "Expression must be true")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "Expression must be true"
    }

    def "checkNotNull accepts a non-null value"() {
        when:
        Preconditions.checkNotNull("non-null value", "This should not be thrown")

        then:
        noExceptionThrown()
    }

    def "checkNotNull rejects a null value"() {
        when:
        Preconditions.checkNotNull(null, "Value cannot be null")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "Value cannot be null"
    }

    def "checkArgument rejects a false complex condition"() {
        when:
        Preconditions.checkArgument(15 >= 18, "Age must be at least 18")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "Age must be at least 18"
    }

    def "checkArgument accepts a true complex condition"() {
        when:
        Preconditions.checkArgument(25 >= 18, "Age must be at least 18")

        then:
        noExceptionThrown()
    }
}
