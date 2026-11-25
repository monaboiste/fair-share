package fairshare.testing

import fairshare.buildlogic.library
import fairshare.buildlogic.libs

plugins {
    id("fairshare.kotlin.jvm-conventions")
}

dependencies {
    testImplementation(platform(libs.library("junit-bom")))
    testRuntimeOnly(libs.library("junit-platform-launcher"))

    testImplementation(platform(libs.library("kotest-bom")))
    testImplementation(libs.library("kotest-runner-junit5"))
    testImplementation(libs.library("kotest-assertions-core"))
    testImplementation(libs.library("kotest-property"))
}

pluginManager.withPlugin("fairshare.spring.kotlin-conventions") {
    dependencies {
        testImplementation("org.springframework.boot:spring-boot")
        testImplementation("org.springframework.boot:spring-boot-autoconfigure")
        testImplementation("org.springframework.boot:spring-boot-test")
        testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
        testImplementation("org.springframework:spring-test")

        testImplementation(libs.library("kotest-extensions-spring"))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
