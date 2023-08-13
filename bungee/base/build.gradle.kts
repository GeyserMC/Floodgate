dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    annotationProcessor(libs.micronaut.inject.java)
    compileOnlyApi(projects.isolation)

    implementation(libs.cloud.bungee)
}

relocate("net.kyori")
relocate("cloud.commandframework")
relocate("io.leangen.geantyref") // used in cloud

// these dependencies are already present on the platform
provided(libs.bungee)
provided(libs.gson)
provided(libs.snakeyaml)
