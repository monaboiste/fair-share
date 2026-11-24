plugins {
    id("fairshare.kotlin.jvm-library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.application"

dependencies {
    implementation(project(":domain"))
}
