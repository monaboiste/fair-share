plugins {
    id("fairshare.spring.application-conventions")
    id("fairshare.observability.tracing-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.bootstrap"

dependencies {
    runtimeOnly(project(":infrastructure"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-webmvc")
    runtimeOnly("org.springframework.boot:spring-boot-starter-validation")

    testImplementation(testFixtures(project(":infrastructure")))
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-tx")
}

tasks.bootJar {
    archiveFileName.set("fair-share.jar")
}
