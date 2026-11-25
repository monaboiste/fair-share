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
}

tasks.bootJar {
    archiveFileName.set("fair-share.jar")
}
