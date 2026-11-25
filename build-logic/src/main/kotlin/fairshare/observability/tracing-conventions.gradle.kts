package fairshare.observability

import fairshare.buildlogic.runtimeOnly

if (!pluginManager.hasPlugin("fairshare.spring.application-conventions")) {
    throw GradleException(
        "tracing-conventions requires fairshare.spring.application-conventions to be applied first.",
    )
}

dependencies {
    runtimeOnly("org.springframework.boot:spring-boot-starter-opentelemetry")
}
