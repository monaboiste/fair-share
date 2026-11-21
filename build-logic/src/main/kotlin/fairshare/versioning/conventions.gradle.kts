package fairshare.versioning

import fairshare.buildlogic.PrintTask
import pl.allegro.tech.build.axion.release.domain.VersionConfig
import pl.allegro.tech.build.axion.release.domain.VersionService

if (project != rootProject) {
    throw GradleException(
        "fairshare.versioning-conventions must be applied to the root project only."
    )
}

pluginManager.apply("pl.allegro.tech.build.axion-release")

val requestedTasks = gradle.startParameter.taskNames
    .map { it.substringAfterLast(':') }
    .toSet()

val finalRequested = "final" in requestedTasks
val devSnapshotRequested = "devSnapshot" in requestedTasks
val versionTaskRequested = finalRequested || devSnapshotRequested
val forcedVersion = providers.gradleProperty("release.version")
val releaseScope = providers.gradleProperty("release.scope")

if (finalRequested && devSnapshotRequested) {
    throw GradleException(
        "'final' and 'devSnapshot' cannot be requested together."
    )
}

if (versionTaskRequested && !releaseScope.isPresent) {
    throw GradleException(
        "Missing required 'release.scope'. Expected one of: patch, minor, major. " +
            "Example: -Prelease.scope=patch"
    )
}

val incrementer = releaseScope.orNull?.let { scope ->
    when (scope) {
        "patch" -> "incrementPatch"
        "minor" -> "incrementMinor"
        "major" -> "incrementMajor"

        else -> throw GradleException(
            "Invalid release.scope '$scope'. Expected one of: patch, minor, major."
        )
    }
}

val scmVersion = extensions.getByType<VersionConfig>()

scmVersion.apply {
    tag.prefix = ""
    sanitizeVersion = true
    releaseOnlyOnReleaseBranches = true
    releaseBranchNames = listOf("main")
    unshallowRepoOnCI = false

    incrementer?.let(::versionIncrementer)

    when {
        forcedVersion.isPresent || finalRequested -> {
            versionCreator("simple")
        }

        else -> {
            versionCreator { version, position ->
                if (position.isSnapshot) {
                    "$version-${position.branch}.${position.shortRevision}"
                } else {
                    version
                }
            }
        }
    }
    snapshotCreator { _, _ -> "" }
}

val resolvedVersion: Provider<VersionService.DecoratedVersion> = scmVersion.versionProvider()
val finalVersion: Provider<String> = resolvedVersion.map(VersionService.DecoratedVersion::getUndecoratedVersion)
val devSnapshotVersion: Provider<String> = resolvedVersion.map(VersionService.DecoratedVersion::getDecoratedVersion)

val selectedVersion: Provider<String> = when {
    finalRequested -> finalVersion
    devSnapshotRequested -> devSnapshotVersion
    else -> resolvedVersion.map(VersionService.DecoratedVersion::getDecoratedVersion)
}

allprojects {
    version = selectedVersion.get()
}

tasks.register<PrintTask>("final") {
    group = "versioning"
    description = "Calculate the final release version."

    lines.add(finalVersion)
}

tasks.register<PrintTask>("devSnapshot") {
    group = "versioning"
    description = "Calculate the development version."

    lines.add(devSnapshotVersion)
}
