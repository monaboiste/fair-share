package fairshare.observability

import fairshare.buildlogic.implementation
import fairshare.buildlogic.library
import fairshare.buildlogic.libs

plugins {
    id("fairshare.kotlin.jvm-conventions")
}

dependencies {
    implementation(libs.library("slf4j"))
}
