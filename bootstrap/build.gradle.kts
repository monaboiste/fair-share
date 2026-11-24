plugins {
    id("fairshare.spring.application-conventions")
    id("fairshare.observability.tracing-conventions")
    id("fairshare.observability.logging-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.bootstrap"

dependencies {
    implementation(project(":infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.xerial:sqlite-jdbc")
    runtimeOnly("tools.jackson.module:jackson-module-kotlin")
}

tasks.bootJar {
    archiveFileName.set("fair-share.jar")
}
