dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    compileOnlyApi(projects.isolation)

    implementation(libs.cloud.bukkit)
    // hack to make pre 1.12 work
    implementation(libs.guava)

    compileOnlyApi(libs.folia.api)
}

relocate("net.kyori")
relocate("cloud.commandframework")
relocate("io.leangen.geantyref") // used in cloud
// hack to make pre 1.12 work
relocate("com.google.common")
relocate("com.google.guava")
// hack to make (old versions? of) Paper work
relocate("it.unimi")

provided(libs.authlib)
provided(libs.gson)
provided(libs.snakeyaml)
// don't include libraries that Floodgate interacts with,
// as it'll otherwise result in a loader constraint violation
provided(libs.netty.transport)
provided(libs.netty.codec)
provided(libs.slf4j)
