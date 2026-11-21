package placeholder.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType


val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias)
        .orElseThrow { GradleException("Library '$alias' not found in version catalog") }

fun VersionCatalog.version(alias: String): String =
    findVersion(alias)
        .map { it.requiredVersion }
        .orElseThrow { GradleException("Version '$alias' not found in version catalog") }


fun DependencyHandlerScope.implementation(dependency: Any) {
    add("implementation", dependency)
}

fun DependencyHandlerScope.runtimeOnly(dependency: Any) {
    add("runtimeOnly", dependency)
}

fun DependencyHandlerScope.testImplementation(dependency: Any) {
    add("testImplementation", dependency)
}

fun DependencyHandlerScope.testRuntimeOnly(dependency: Any) {
    add("testRuntimeOnly", dependency)
}
