import fairshare.buildlogic.implementation

plugins {
    id("fairshare.observability.logging-conventions")
    id("fairshare.spring.library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.infrastructure"

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-core")

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation("jakarta.validation:jakarta.validation-api")

    testImplementation("org.xerial:sqlite-jdbc")

    testRuntimeOnly("org.springframework.boot:spring-boot-starter-validation")
}
