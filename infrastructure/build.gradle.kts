import fairshare.buildlogic.implementation
import org.liquibase.gradle.LiquibaseExtension
import java.nio.file.Path

buildscript {
    dependencies {
        classpath(libs.liquibase.core)
    }
}

plugins {
    alias(libs.plugins.liquibase)
    id("fairshare.observability.logging-conventions")
    id("fairshare.spring.library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.infrastructure"

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")

    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")

    implementation("org.xerial:sqlite-jdbc")

    implementation("jakarta.validation:jakarta.validation-api")

    runtimeOnly("tools.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.hamcrest:hamcrest")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(libs.liquibase.core)
    testRuntimeOnly("org.springframework.boot:spring-boot-starter-validation")

    add("liquibaseRuntime", platform(libs.spring.boot.bom))
    add("liquibaseRuntime", libs.liquibase.core)
    add("liquibaseRuntime", libs.picocli)
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
        setArguments(
            mapOf(
                "url" to databaseUrl,
                "searchPath" to layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
                "changelogFile" to "db/changelog/db.changelog-master.yaml",
            ),
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
