package placeholder.observability

import placeholder.buildlogic.implementation

if (!pluginManager.hasPlugin("placeholder.spring.application-conventions")) {
    throw GradleException(
        "tracing-conventions requires placeholder.spring.application-conventions to be applied first.",
    )
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
}
