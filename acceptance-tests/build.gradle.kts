plugins {
    id("fairshare.testing.conventions")
    id("fairshare.quality.formatting-conventions")
    id("fairshare.quality.static-analysis-conventions")
}

group = "fairshare.acceptance"

dependencies {
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation("tools.jackson.core:jackson-databind")
}

tasks.test {
    enabled = providers.gradleProperty("acceptance").isPresent
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("Tests run against an externally managed application") { true }
}
