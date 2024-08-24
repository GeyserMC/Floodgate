dependencies {
    api(projects.coreNetty4)
    annotationProcessor(projects.coreNetty4)
    annotationProcessor(libs.micronaut.inject.java)
    compileOnlyApi(projects.isolation)

    implementation(libs.cloud.bungee)
    implementation(libs.adventure.platform.bungee)
}

relocate("net.kyori")
relocate("org.incendo.cloud")
relocate("io.leangen.geantyref") // used in cloud

// these dependencies are already present on the platform
provided(libs.bungee)
provided(libs.gson)
provided(libs.snakeyaml)
