// Gradle's dependencyResolutionManagement repository API is still marked @Incubating.
// Remove this suppression once Gradle promotes this API to stable and the project
// minimum Gradle version includes that change.
// Tracking issue: https://github.com/gradle/gradle/issues/32443
@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
