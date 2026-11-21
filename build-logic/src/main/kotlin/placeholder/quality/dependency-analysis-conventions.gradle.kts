package placeholder.quality

import com.autonomousapps.DependencyAnalysisSubExtension
import placeholder.buildlogic.printAfter

plugins {
    id("com.autonomousapps.dependency-analysis")
}

extensions.configure<DependencyAnalysisSubExtension> {
    issues {
        ignoreSourceSet("test")

        onAny {
            severity("warn")
        }
    }
}


val dependencyAnalysisReport = layout.buildDirectory.file("reports/dependency-analysis/project-health-report.txt")

tasks.printAfter(
    "projectHealth",
    providers.provider { "Dependency analysis results:" },
    dependencyAnalysisReport.map { "Report:\t${it.asFile.toURI()}" },
)

tasks.register("dependencyAnalysisCheck") {
    group = "verification"
    description = "Check dependency declarations"

    dependsOn(tasks.named("projectHealth"))
}
