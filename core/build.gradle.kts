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

    api(libs.micronaut.inject)
    annotationProcessor(libs.micronaut.inject.java)
    api(libs.micronaut.context)
    api(libs.micronaut.http.client)
    api(libs.micronaut.validation)
    annotationProcessor(libs.micronaut.validation.processor)

    api(libs.micronaut.serde.jsonp)
    compileOnlyApi(libs.jakarta.jsonb)
    annotationProcessor(libs.micronaut.serde.processor)

    api(libs.micronaut.data.jdbc)
    runtimeOnly(libs.micronaut.hikari)
    annotationProcessor(libs.micronaut.data.processor)
    compileOnlyApi(libs.jakarta.persistence)

    runtimeOnly("com.h2database:h2")
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
