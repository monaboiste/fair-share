plugins {
    id("fairshare.kotlin.jvm-library-conventions")
    id("fairshare.quality.conventions")
}

group = "fairshare.domain"

dependencies {
    api(libs.joda.money)
}
