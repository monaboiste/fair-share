plugins {
    id("fairshare.spring.application-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.bootstrap"

configurations.consumable("packagedApplication")

artifacts {
    add("packagedApplication", tasks.named("bootJar"))
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    runtimeOnly("org.springframework.boot:spring-boot-starter-webmvc")
    runtimeOnly("org.springframework.boot:spring-boot-starter-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework:spring-context")
    implementation("org.xerial:sqlite-jdbc")

    testImplementation("org.springframework:spring-beans")
    testImplementation("org.springframework:spring-core")
}
