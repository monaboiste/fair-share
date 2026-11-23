plugins {
    `java-library`
    id("fairshare.kotlin.jvm-library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.infrastructure"

dependencies {
    api(project(":application"))
    api(project(":domain"))

    implementation(platform(libs.spring.boot.bom))
    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-web")
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")
    runtimeOnly("org.xerial:sqlite-jdbc")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework:spring-test")
    testRuntimeOnly("org.springframework:spring-webmvc")
}
