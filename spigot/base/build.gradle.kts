dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    annotationProcessor(libs.micronaut.inject.java)
    compileOnlyApi(projects.isolation)

    implementation(libs.cloud.bukkit)

    compileOnlyApi(libs.paper.api)
}

relocate("net.kyori")
relocate("cloud.commandframework")
relocate("io.leangen.geantyref") // used in cloud
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
