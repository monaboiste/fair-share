package com.softwarearchetypes.common

import spock.lang.Specification

class VersionSpec extends Specification {

    def "creates the initial version with value zero"() {
        expect:
        Version.initial().value() == 0L
    }

    def "creates a version with the requested value"() {
        expect:
        Version.of(value).value() == value

        where:
        value << [42L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE]
    }

    def "versions with the same value are equal"() {
        given:
        def firstVersion = Version.of(10L)
        def secondVersion = Version.of(10L)

        expect:
        firstVersion == secondVersion
        firstVersion.hashCode() == secondVersion.hashCode()
    }

    def "versions with different values are not equal"() {
        expect:
        Version.of(10L) != Version.of(20L)
    }

    def "has the record string representation"() {
        expect:
        Version.of(123L).toString() == "Version[value=123]"
    }

    def "the initial version equals version zero"() {
        expect:
        Version.initial() == Version.of(0L)
    }

    def "multiple initial versions have the same value"() {
        given:
        def firstInitial = Version.initial()
        def secondInitial = Version.initial()

        expect:
        firstInitial == secondInitial
        firstInitial.value() == 0L
        secondInitial.value() == 0L
    }
}
