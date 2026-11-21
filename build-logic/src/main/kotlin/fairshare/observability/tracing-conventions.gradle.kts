package fairshare.observability

import fairshare.buildlogic.implementation

if (!pluginManager.hasPlugin("fairshare.spring.application-conventions")) {
    throw GradleException(
        "tracing-conventions requires fairshare.spring.application-conventions to be applied first.",
    )
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
}
