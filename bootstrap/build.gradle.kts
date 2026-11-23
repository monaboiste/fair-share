plugins {
    id("fairshare.spring.application-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.bootstrap"

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    runtimeOnly("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework:spring-beans")
    implementation("org.springframework:spring-context")

    testImplementation("tools.jackson.core:jackson-databind")
}
