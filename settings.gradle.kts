// Gradle's dependencyResolutionManagement repository API is still marked @Incubating.
// Remove this suppression once Gradle promotes this API to stable and the project
// minimum Gradle version includes that change.
// Tracking issue: https://github.com/gradle/gradle/issues/32443
@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

rootProject.name = "placeholder"

include("app")
