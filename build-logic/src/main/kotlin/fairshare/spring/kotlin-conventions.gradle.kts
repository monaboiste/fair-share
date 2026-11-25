package fairshare.spring

import fairshare.buildlogic.implementation
import fairshare.buildlogic.library
import fairshare.buildlogic.libs

plugins {
    id("fairshare.kotlin.jvm-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation(libs.library("kotlin-reflect"))

    implementation(platform(libs.library("spring-boot-bom")))
    implementation("org.springframework:spring-context")
}
