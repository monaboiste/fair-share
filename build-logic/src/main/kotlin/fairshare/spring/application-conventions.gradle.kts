package fairshare.spring

import fairshare.buildlogic.implementation
import fairshare.buildlogic.library
import fairshare.buildlogic.libs

plugins {
    id("fairshare.spring.kotlin-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(platform(libs.library("spring-boot-bom")))
    implementation(libs.library("spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

springBoot {
    buildInfo {
        // build.time changes on every build, preventing the task from being up-to-date
        // and making the generated artifact less reproducible.
        excludes.set(setOf("time"))
    }
}
