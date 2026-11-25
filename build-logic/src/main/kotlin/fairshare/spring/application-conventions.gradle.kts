package fairshare.spring

import fairshare.buildlogic.implementation
import fairshare.buildlogic.library
import fairshare.buildlogic.libs

plugins {
    id("fairshare.spring.kotlin-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}

springBoot {
    buildInfo {
        // build.time changes on every build, preventing the task from being up-to-date
        // and making the generated artifact less reproducible.
        excludes.set(setOf("time"))
    }
}
