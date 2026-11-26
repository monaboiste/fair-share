package fairshare.persistence

import fairshare.buildlogic.library
import fairshare.buildlogic.libs
import org.liquibase.gradle.LiquibaseExtension
import java.nio.file.Path

plugins {
    id("org.liquibase.gradle")
}

dependencies {
    liquibaseRuntime(platform(libs.library("spring-boot-bom")))
    liquibaseRuntime(libs.library("liquibase-core"))
    liquibaseRuntime(libs.library("picocli"))
    liquibaseRuntime("org.xerial:sqlite-jdbc")
}

val protectedArguments = setOf(
    "liquibaseUrl",
    "liquibaseChangelogFile",
    "liquibaseSearchPath",
)

val overriddenArguments = providers
    .gradlePropertiesPrefixedBy("liquibase")
    .map { it.keys.intersect(protectedArguments) }

val databaseUrl = providers.gradleProperty("databasePath")
    .orElse("")
    .zip(overriddenArguments) { configuredPath, overrides ->
        require(overrides.isEmpty()) {
            "Overriding migration arguments is not allowed: ${overrides.sorted().joinToString()}"
        }

        val path = Path.of(configuredPath)
        require(path.isAbsolute) {
            "Set -PdatabasePath to an absolute SQLite database file path"
        }

        "jdbc:sqlite:${path.toUri().toASCIIString()}"
    }

configure<LiquibaseExtension> {
    activities.register("main") {
        arguments = mapOf(
            "url" to databaseUrl,
            "searchPath" to layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
            "changelogFile" to "db/changelog/db.changelog-master.yaml",
        )
    }
}

val updateDatabase = tasks.named("update") {
    notCompatibleWithConfigurationCache("Liquibase initializes its command arguments when the plugin is applied")
}

tasks.register("migrateDatabase") {
    group = "Liquibase"
    description = "Provision the event schema at the absolute SQLite file path supplied with -PdatabasePath"
    dependsOn(updateDatabase)
}
