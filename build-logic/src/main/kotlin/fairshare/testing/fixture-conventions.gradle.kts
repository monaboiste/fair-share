package fairshare.testing

import fairshare.buildlogic.library
import fairshare.buildlogic.libs

plugins {
    id("fairshare.kotlin.jvm-conventions")
    id("java-test-fixtures")
}

pluginManager.withPlugin("fairshare.spring.kotlin-conventions") {
    dependencies {
        testFixturesImplementation(platform(libs.library("spring-boot-bom")))
    }
}
