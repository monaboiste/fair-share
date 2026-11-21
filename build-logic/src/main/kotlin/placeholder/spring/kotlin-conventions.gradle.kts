package placeholder.spring

import placeholder.buildlogic.implementation
import placeholder.buildlogic.library
import placeholder.buildlogic.libs

plugins {
    id("placeholder.kotlin.jvm-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
}


dependencies {
    implementation(platform(libs.library("spring-boot-dependencies")))
    implementation(libs.library("kotlin-reflect"))
}
