package placeholder.kotlin

import placeholder.buildlogic.libs
import placeholder.buildlogic.version
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = providers.gradleProperty("project.group").get()

val javaVersion = libs.version("java")

kotlin {
    jvmToolchain(javaVersion.toInt())

    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion)
        allWarningsAsErrors = true
        extraWarnings = true
    }
}
