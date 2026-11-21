package fairshare.quality

import com.diffplug.gradle.spotless.SpotlessExtension
import fairshare.buildlogic.libs
import fairshare.buildlogic.version

plugins {
    id("com.diffplug.spotless")
}

val ktlintVersion = libs.version("ktlint")

configure<SpotlessExtension> {
    isEnforceCheck = false

    kotlin {
        target("src/**/*.kt")

        ktlint(ktlintVersion)
    }

    kotlinGradle {
        target("*.gradle.kts")

        ktlint(ktlintVersion)
    }

    groovy {
        target("src/**/*.groovy")

        greclipse()
    }
}

tasks.register("formatCheck") {
    group = "verification"
    description = "Check for format violations"

    dependsOn(tasks.named("spotlessCheck"))
}

tasks.register("format") {
    group = "quality"
    description = "Apply formatting"

    dependsOn(tasks.named("spotlessApply"))
}
