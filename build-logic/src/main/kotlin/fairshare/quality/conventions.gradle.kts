package fairshare.quality

plugins {
    id("fairshare.quality.formatting-conventions")
    id("fairshare.quality.static-analysis-conventions")
    id("fairshare.quality.coverage-conventions")
    id("fairshare.quality.mutation-testing-conventions")
    id("fairshare.quality.dependency-analysis-conventions")
}

tasks.register("verify") {
    group = "verification"
    description = "Run fast checks suitable for local development and pre-commit"

    dependsOn(
        "formatCheck",
        "staticAnalysisCheck",
    )
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Run the complete quality suite"

    dependsOn(
        "verify",
        "dependencyAnalysisCheck",
        "coverageCheck",
        "mutationCheck",
    )
}
