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

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")

    implementation("org.xerial:sqlite-jdbc")

    implementation("jakarta.validation:jakarta.validation-api")

    runtimeOnly("tools.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.hamcrest:hamcrest")
    testImplementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations")

    testRuntimeOnly("org.springframework.boot:spring-boot-starter-validation")
}
