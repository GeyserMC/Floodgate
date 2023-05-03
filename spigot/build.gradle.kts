dependencies {
    api(projects.core)
    annotationProcessor(projects.core)

    implementation(projects.isolation)

    implementation(libs.cloud.bukkit)
    // hack to make pre 1.12 work
    implementation(libs.guava)

    compileOnlyApi(libs.folia.api)
}

relocate("com.google.inject")
relocate("net.kyori")
relocate("cloud.commandframework")
relocate("io.leangen.geantyref") // used in cloud
// hack to make pre 1.12 work
relocate("com.google.common")
relocate("com.google.guava")
// hack to make (old versions? of) Paper work
relocate("it.unimi")

// these dependencies are already present on the platform
provided(libs.authlib)
provided(libs.netty.transport)
provided(libs.netty.codec)
provided(libs.gson)
provided(libs.snakeyaml)
