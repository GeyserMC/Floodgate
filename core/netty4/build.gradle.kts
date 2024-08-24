dependencies {
    api(projects.coreCommon)
    annotationProcessor(projects.coreCommon)
    annotationProcessor(libs.micronaut.inject.java)
}

// present on all platforms
provided(libs.netty.transport)
provided(libs.netty.codec)