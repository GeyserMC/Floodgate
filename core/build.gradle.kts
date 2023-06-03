plugins {
    id("floodgate.generate-templates")
    id("floodgate.dependency-hash")
    id("io.micronaut.library")
}

dependencies {
    api(projects.api)
    api(libs.base.api)
    compileOnlyApi(projects.isolation)
    api(libs.config.utils)
    annotationProcessor(libs.config.utils.ap)

    api(libs.bundles.fastutil)
    api(libs.java.websocket)
    api(libs.cloud.core)
    api(libs.snakeyaml)
    api(libs.bstats)

    api(libs.guava)

    annotationProcessor(libs.micronaut.inject)
    api(libs.micronaut.inject)
    api(libs.micronaut.context)
    api(libs.micronaut.http.client)
    api(libs.micronaut.validation)

    api(libs.micronaut.serde.jsonp)
    compileOnlyApi(libs.jsonb.annotations)
    annotationProcessor(libs.micronaut.serde.processor)

    //todo add hibernate dependency back in core,
    // it's not possible to make it optional as the service files would be messed up
    api(projects.database)

    annotationProcessor(libs.micronaut.data.processor)
//    implementation("io.micronaut.data:micronaut-data-model")
//    implementation("jakarta.persistence:jakarta.persistence-api:2.2.3")
}

// present on all platforms
provided(libs.netty.transport)
provided(libs.netty.codec)

relocate("org.bstats")

tasks {
    templateSources {
        replaceToken("fullVersion", fullVersion())
        replaceToken("version", version)
        replaceToken("branch", branchName())
        replaceToken("buildNumber", buildNumber())
    }
}
