package com.softwarearchetypes.product

import spock.lang.Specification

class SerialNumberSpec extends Specification {

    def "textual serial number preserves value and reports type"() {
        given:
        SerialNumber serialNumber = SerialNumber.of(" SN-001 ")

        expect:
        serialNumber.value() == " SN-001 "
        serialNumber.toString() == " SN-001 "
        serialNumber.type() == "TEXTUAL"
    }

    def "vin normalizes separators and case and reports type"() {
        when:
        SerialNumber serialNumber = SerialNumber.vin("1hg-bh41j-xmn109186")

        then:
        serialNumber.value() == "1HGBH41JXMN109186"
        serialNumber.type() == "VIN"
    }

    def "valid imei values are accepted and normalized"() {
        expect:
        SerialNumber.imei(value).value() == normalized
        SerialNumber.imei(value).type() == "IMEI"

        where:
        value                 | normalized
        "490154203237518"     | "490154203237518"
        "49015-420323-7518"   | "490154203237518"
    }

    def "null textual serial number is rejected"() {
        when:
        SerialNumber.of(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "invalid serial number values are rejected"() {
        when:
        switch (kind) {
            case "text" -> SerialNumber.of(value)
            case "vin" -> SerialNumber.vin(value)
            case "imei" -> SerialNumber.imei(value)
        }

        then:
        thrown(IllegalArgumentException)

        where:
        kind   | value
        "text" | ""
        "text" | "   "
        "vin"  | "1HGBH41JXMN10918"
        "vin"  | "1HGBH41JXMN109186I"
        "vin"  | "1HGBH41JXMN10918Q"
        "imei" | ""
        "imei" | "490154203237519"
        "imei" | "49015420323751"
        "imei" | "49015420323751A"
    }
}
