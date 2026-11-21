package placeholder.quality

import placeholder.buildlogic.printAfter
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("org.jetbrains.kotlinx.kover")
}

kover {
    reports {
        total {
            verify {
                onCheck = false

                rule("line coverage") {
                    minBound(
                        minValue = 90,
                        coverageUnits = CoverageUnit.LINE,
                    )
                }

                rule("branch coverage") {
                    minBound(
                        minValue = 80,
                        coverageUnits = CoverageUnit.BRANCH,
                    )
                }
            }
        }
        filters {
            excludes {
                classes("*MainApplicationKt")
            }
        }
    }
}

val htmlReport = layout.buildDirectory.file("reports/kover/html/index.html")
val xmlReport = layout.buildDirectory.file("reports/kover/report.xml")

tasks.printAfter(
    "koverVerify",
    providers.provider { "Code coverage results:" },
    htmlReport.map { "HTML:\t${it.asFile.toURI()}" },
    xmlReport.map { "XML:\t${it.asFile.toURI()}" },
)

tasks.register("coverageCheck") {
    group = "verification"
    description = "Verify test coverage and generate coverage reports"

    dependsOn(
        tasks.named("koverVerify"),
        tasks.named("koverHtmlReport"),
        tasks.named("koverXmlReport"),
    )
}
