plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.pitest.gradle.plugin)
    implementation(libs.dependency.analysis.gradle.plugin)

    implementation(libs.axion.gradle.plugin)

    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.kotlin.spring.gradle.plugin)
}
