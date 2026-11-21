package placeholder.quality

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import placeholder.buildlogic.printAfter

plugins {
    id("placeholder.kotlin.jvm-conventions")
    id("dev.detekt")
}

val detektConfig = rootProject.layout.projectDirectory
    .file("build-logic/config/detekt/detekt.yml")

extensions.configure<DetektExtension> {
    basePath.set(rootProject.projectDir)
    config.setFrom(detektConfig)

    buildUponDefaultConfig = true
    allRules = false

    ignoreFailures = false
    failOnSeverity = FailOnSeverity.Error

    parallel = true
}

val reportDir = layout.buildDirectory.dir("reports/detekt")
val checkstyleReport = reportDir.map { it.file("detektMain.xml") }
val htmlReport = reportDir.map { it.file("detektMain.html") }
val sarifReport = reportDir.map { it.file("detektMain.sarif") }
val markdownReport = reportDir.map { it.file("detektMain.md") }

tasks.named<Detekt>("detektMain").configure {
    reports {
        checkstyle {
            required.set(true)
            outputLocation.set(checkstyleReport)
        }
        html {
            required.set(true)
            outputLocation.set(htmlReport)
        }
        sarif {
            required.set(true)
            outputLocation.set(sarifReport)
        }
        markdown {
            required.set(true)
            outputLocation.set(markdownReport)
        }
    }
}
tasks.printAfter(
    "detektMain",
    providers.provider { "Static code analysis results:" },
    checkstyleReport.map { "Checkstyle:\t${it.asFile.toURI()}" },
    htmlReport.map { "HTML:\t\t${it.asFile.toURI()}" },
    sarifReport.map { "SARIF:\t\t${it.asFile.toURI()}" },
    markdownReport.map { "Markdown:\t${it.asFile.toURI()}" },
)

tasks.named("check").configure {
    setDependsOn(
        dependsOn.filterNot {
            it is TaskProvider<*> && it.name == "detekt"
        },
    )
}

tasks.register("staticAnalysisCheck") {
    group = "verification"
    description = "Run static code analysis"

    dependsOn(tasks.named("detektMain"))
}
