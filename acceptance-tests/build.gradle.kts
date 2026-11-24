import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("fairshare.testing.conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.acceptance"

val packagedApplication = configurations.create("packagedApplication") {
    isCanBeConsumed = false
}

dependencies {
    add(packagedApplication.name, project(path = ":bootstrap", configuration = "packagedApplication"))
    testImplementation(platform(libs.spring.boot.bom))
    testImplementation("tools.jackson.core:jackson-databind")
}

tasks.test {
    jvmArgumentProviders.add(PackagedApplicationArgument(packagedApplication))
}

class PackagedApplicationArgument(
    @get:InputFiles @get:PathSensitive(PathSensitivity.ABSOLUTE) val jar: FileCollection,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("-Dfair-share.test.jar=${jar.singleFile.absolutePath}")
}
