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
    implementation("org.springframework.boot:spring-boot-jackson")
    runtimeOnly("org.springframework.boot:spring-boot-starter-webmvc")
    runtimeOnly("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-context")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.springframework:spring-beans")
    testImplementation("org.springframework:spring-core")
}
