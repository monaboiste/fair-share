plugins {
    `java-library`
    id("fairshare.kotlin.jvm-library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.application"

dependencies {
    api(project(":domain"))
}
