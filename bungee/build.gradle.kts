dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    implementation(projects.isolation)

    implementation(libs.cloud.bungee)
}

relocate("net.kyori")
relocate("cloud.commandframework")
// used in cloud
relocate("io.leangen.geantyref")

// these dependencies are already present on the platform
provided(libs.bungee)
provided(libs.gson)
provided(libs.guava)
provided(libs.snakeyaml)
