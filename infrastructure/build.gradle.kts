plugins {
    id("fairshare.spring.library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.infrastructure"

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(libs.slf4j)
    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("com.fasterxml.jackson.core:jackson-annotations")
    testImplementation("jakarta.validation:jakarta.validation-api")
    testImplementation("org.springframework.boot:spring-boot")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.xerial:sqlite-jdbc")
    testImplementation("tools.jackson.module:jackson-module-kotlin")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-validation")
}
