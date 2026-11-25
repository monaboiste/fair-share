package fairshare.quality

import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.DependencyAnalysisSubExtension
import fairshare.buildlogic.printAfter

plugins {
    id("com.autonomousapps.dependency-analysis")
}

val isRootProject = project == rootProject

if (isRootProject) {
    extensions.configure<DependencyAnalysisExtension> {

        // Dependency bundles must be declared in the root project only
        structure {
            bundle("spring-boot-core") {
                includeDependency("org.springframework.boot:spring-boot-starter")
                includeDependency("org.springframework.boot:spring-boot")
                includeDependency("org.springframework.boot:spring-boot-autoconfigure")

                includeDependency("org.springframework:spring-core")
                includeDependency("org.springframework:spring-context")
                includeDependency("org.springframework:spring-beans")
                includeDependency("org.springframework:spring-expression")
                includeDependency("org.springframework:spring-aop")
            }

            bundle("spring-webmvc") {
                includeDependency("org.springframework.boot:spring-boot-starter-webmvc")
                includeDependency("org.springframework.boot:spring-boot-webmvc")
                includeDependency("org.springframework.boot:spring-boot-http-converter")

                includeDependency("org.springframework:spring-web")
                includeDependency("org.springframework:spring-webmvc")
            }

            bundle("spring-boot-logging") {
                includeDependency("org.springframework.boot:spring-boot-starter")
                includeDependency("org.springframework.boot:spring-boot-starter-logging")

                includeDependency("ch.qos.logback:logback-classic")
                includeDependency("ch.qos.logback:logback-core")
                includeDependency("org.apache.logging.log4j:log4j-api")
                includeDependency("org.apache.logging.log4j:log4j-to-slf4j")
                includeDependency("org.slf4j:jul-to-slf4j")
            }

            bundle("spring-boot-test") {
                includeDependency("org.springframework.boot:spring-boot-starter-test")
                includeDependency("org.springframework.boot:spring-boot-starter-webmvc-test")
                includeDependency("org.springframework.boot:spring-boot-starter-jackson-test")

                includeDependency("org.springframework.boot:spring-boot-test")
                includeDependency("org.springframework.boot:spring-boot-test-autoconfigure")
                includeDependency("org.springframework.boot:spring-boot-webmvc-test")
                includeDependency("org.springframework.boot:spring-boot-jackson-test")
                includeDependency("org.springframework.boot:spring-boot-resttestclient")

                includeDependency("org.springframework:spring-test")
            }

            bundle("jackson") {
                includeDependency("org.springframework.boot:spring-boot-starter-jackson")
                includeDependency("org.springframework.boot:spring-boot-jackson")

                includeDependency("tools.jackson.core:jackson-core")
                includeDependency("tools.jackson.core:jackson-databind")
                includeDependency("com.fasterxml.jackson.core:jackson-annotations")
            }

            bundle("validation") {
                includeDependency("org.springframework.boot:spring-boot-starter-validation")
                includeDependency("org.springframework.boot:spring-boot-validation")

                includeDependency("jakarta.validation:jakarta.validation-api")
                includeDependency("org.hibernate.validator:hibernate-validator")
                includeDependency("org.apache.tomcat.embed:tomcat-embed-el")
            }

            bundle("actuator") {
                includeDependency("org.springframework.boot:spring-boot-starter-actuator")
                includeDependency("org.springframework.boot:spring-boot-actuator")
                includeDependency("org.springframework.boot:spring-boot-actuator-autoconfigure")
                includeDependency("org.springframework.boot:spring-boot-health")
            }

            bundle("opentelemetry") {
                includeDependency("org.springframework.boot:spring-boot-starter-opentelemetry")
                includeDependency("org.springframework.boot:spring-boot-opentelemetry")
                includeDependency("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")

                includeDependency("io.micrometer:micrometer-tracing-bridge-otel")
                includeDependency("io.micrometer:micrometer-registry-otlp")
                includeDependency("io.opentelemetry:opentelemetry-exporter-otlp")
            }
        }
    }
} else {
    extensions.configure<DependencyAnalysisSubExtension> {
        issues {
            ignoreSourceSet("test")

            onAny {
                severity("warn")
            }
        }

    }
}

val healthTask = if (isRootProject) "buildHealth" else "projectHealth"
val reportFilename = if (isRootProject) "build-health-report.txt" else "project-health-report.txt"
val dependencyAnalysisReport = layout.buildDirectory.file("reports/dependency-analysis/${reportFilename}")

tasks.printAfter(
    healthTask,
    providers.provider { "Dependency analysis results:" },
    dependencyAnalysisReport.map { "Report:\t${it.asFile.toURI()}" },
)

tasks.register("dependencyAnalysisCheck") {
    group = "verification"
    description = "Check dependency declarations"

    dependsOn(tasks.named(healthTask))
}
