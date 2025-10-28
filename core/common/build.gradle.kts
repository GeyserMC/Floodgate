plugins {
    id("floodgate.publish-conventions")
    id("floodgate.generate-templates")
    id("floodgate.dependency-hash")
    id("floodgate.shadow-conventions")
    id("io.micronaut.library")
}

dependencies {
    api(projects.api)
    compileOnlyApi(libs.base.api)
    compileOnlyApi(projects.isolation)

    annotationProcessor(libs.configurate.`interface`.ap)
    api(libs.configurate.`interface`)
    implementation(libs.configurate.yaml)

    annotationProcessor(libs.database.utils.ap)
    api(libs.database.utils)
    compileOnlyApi(libs.database.utils.sql)

    api(libs.gson)
    api(libs.bundles.fastutil)
    api(libs.cloud.core)
    api(libs.snakeyaml)
    api(libs.bstats)

    api(libs.adventure.text.minimessage)

    api(libs.micronaut.inject)
    annotationProcessor(libs.micronaut.inject.java)
    api(libs.micronaut.context)

    //todo re-add validation
    api(libs.avaje.http.client)
    api(libs.avaje.http.api)
    annotationProcessor(libs.avaje.http.client.generator)

    implementation(libs.avaje.jsonb)
    annotationProcessor(libs.avaje.jsonb.generator)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    api(libs.expiringmap)
}

providedDependency(libs.slf4j)

tasks {
    templateSources {
        replaceToken("fullVersion", fullVersion())
        replaceToken("version", version)
        replaceToken("branch", branchName())
        replaceToken("buildNumber", buildNumber())
    }

    test { useJUnitPlatform() }
}
