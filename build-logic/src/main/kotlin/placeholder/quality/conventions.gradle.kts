package placeholder.quality

plugins {
    id("placeholder.quality.formatting-conventions")
    id("placeholder.quality.static-analysis-conventions")
    id("placeholder.quality.coverage-conventions")
    id("placeholder.quality.mutation-testing-conventions")
    id("placeholder.quality.dependency-analysis-conventions")
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
