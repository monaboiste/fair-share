package placeholder.quality

import placeholder.buildlogic.library
import placeholder.buildlogic.libs
import placeholder.buildlogic.printAfter
import placeholder.buildlogic.version
import info.solidsoft.gradle.pitest.PitestTask
import java.io.OutputStream

plugins {
    id("placeholder.testing.conventions")
    id("info.solidsoft.pitest")
}

dependencies {
    pitest(platform(libs.library("kotest-bom")))
    pitest(libs.library("kotest-extensions-pitest"))
}

pitest {
    pitestVersion = libs.version("pitest-engine")
    junit5PluginVersion = libs.version("pitest-junit")

    outputFormats = setOf("HTML", "XML")
    timestampedReports = false

    mutators = setOf("DEFAULTS")

    mutationThreshold = 85
    testStrengthThreshold = 90

    threads = Runtime.getRuntime().availableProcessors()

    failWhenNoMutations = false

    verbosity = "QUIET"

    excludedClasses = setOf("*MainApplicationKt")
}

tasks.named<PitestTask>("pitest") {
    // suppress output
    doFirst {
        standardOutput = OutputStream.nullOutputStream()
    }
}

val mutationHtmlReport = layout.buildDirectory.file("reports/pitest/index.html")
val mutationXmlReport = layout.buildDirectory.file("reports/pitest/mutations.xml")

tasks.printAfter(
    "pitest",
    providers.provider { "Mutation testing results:" },
    mutationHtmlReport.map { "HTML:\t${it.asFile.toURI()}" },
    mutationXmlReport.map { "XML:\t${it.asFile.toURI()}" },
)

tasks.register("mutationCheck") {
    group = "verification"
    description = "Run mutation testing"

    dependsOn(tasks.named("pitest"))
}
