plugins {
    id("fairshare.spring.library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.infrastructure"

dependencies {
    implementation(project(":application"))
    implementation("org.springframework:spring-web")
}
