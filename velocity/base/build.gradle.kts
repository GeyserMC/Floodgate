dependencies {
    api(projects.coreNetty4)
    annotationProcessor(projects.coreNetty4)
    annotationProcessor(libs.micronaut.inject.java)

    implementation(libs.cloud.velocity)
}

relocate("org.incendo.cloud")
relocate("io.leangen.geantyref") // used in cloud

relocate("org.yaml.snakeyaml")

// these dependencies are already present on the platform
provided(libs.gson)
provided(libs.velocity.api)
provided(libs.velocity.proxy)
providedDependency(libs.slf4j)
providedDependency(libs.adventure.api)
providedDependency(libs.adventure.key)
