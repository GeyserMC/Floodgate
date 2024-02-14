plugins {
    id("floodgate.generate-templates")
    id("floodgate.dependency-hash")
    id("io.micronaut.library")
}

dependencies {
    api(projects.api)
    api(libs.base.api)
    compileOnlyApi(projects.isolation)

    annotationProcessor(libs.configurate.`interface`.ap)
    api(libs.configurate.`interface`)
    implementation(libs.configurate.yaml)

    annotationProcessor(libs.database.utils.ap)
    api(libs.database.utils)
    compileOnlyApi(libs.database.utils.sql)

    api(libs.bundles.fastutil)
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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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

    test { useJUnitPlatform() }
}
