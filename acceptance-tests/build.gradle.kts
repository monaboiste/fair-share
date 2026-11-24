plugins {
    id("fairshare.testing.conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.acceptance"

dependencies {
    testImplementation(project(":bootstrap"))
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation("org.springframework.boot:spring-boot")
    testImplementation("org.springframework:spring-context")
    testImplementation("org.springframework:spring-core")
    testImplementation("tools.jackson.core:jackson-databind")
    testRuntimeOnly("org.xerial:sqlite-jdbc")
}
