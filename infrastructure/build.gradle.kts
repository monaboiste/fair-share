plugins {
    id("fairshare.spring.library-conventions")
    id("fairshare.observability.logging-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.infrastructure"

dependencies {
    api(project(":application"))
    api(project(":domain"))
    api("org.springframework:spring-context")

    implementation("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")
    runtimeOnly("tools.jackson.module:jackson-module-kotlin")

    testCompileOnly("jakarta.servlet:jakarta.servlet-api")
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("ch.qos.logback:logback-core")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.xerial:sqlite-jdbc")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-validation")
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-webmvc")
}
