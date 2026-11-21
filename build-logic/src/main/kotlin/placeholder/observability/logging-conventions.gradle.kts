package placeholder.observability

import placeholder.buildlogic.implementation
import placeholder.buildlogic.library
import placeholder.buildlogic.libs

plugins {
    id("placeholder.kotlin.jvm-conventions")
}

dependencies {
    implementation(libs.library("slf4j"))
}
