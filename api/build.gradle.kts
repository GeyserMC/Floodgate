plugins {
    id("floodgate.publish-conventions")
    id("floodgate.shadow-conventions")
}

dependencies {
    api(libs.cumulus)
    api(libs.events)
}
