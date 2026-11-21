package placeholder.spring

import placeholder.buildlogic.implementation
import placeholder.buildlogic.library
import placeholder.buildlogic.libs

plugins {
    id("placeholder.spring.kotlin-conventions")
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
