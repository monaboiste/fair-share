package fairshare.migrations

import fairshare.buildlogic.library
import fairshare.buildlogic.libs
import org.liquibase.gradle.LiquibaseExtension
import java.nio.file.Path

plugins {
    id("org.liquibase.gradle")
}

dependencies {
    add("liquibaseRuntime", platform(libs.library("spring-boot-bom")))
    add("liquibaseRuntime", libs.library("liquibase-core"))
    add("liquibaseRuntime", libs.library("picocli"))
    add("liquibaseRuntime", "org.xerial:sqlite-jdbc")
}

val overridesUrl = providers.gradleProperty("liquibaseUrl").map { true }.orElse(false)
val databaseUrl = providers.gradleProperty("databasePath").orElse("").zip(overridesUrl) { configuredPath, override ->
    require(!override) { "Use -PdatabasePath instead of -PliquibaseUrl" }
    val path = Path.of(configuredPath)
    require(path.isAbsolute) { "Set -PdatabasePath to an absolute SQLite database file path" }
    "jdbc:sqlite:${path.toUri().toASCIIString()}"
}

configure<LiquibaseExtension> {
    activities.register("eventSchema") {
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
