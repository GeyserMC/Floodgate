plugins {
    id("floodgate.publish-conventions")
    id("floodgate.shadow-conventions")
}

dependencies {
    api(projects.coreNetty4)
    annotationProcessor(projects.coreNetty4)
    annotationProcessor(libs.micronaut.inject.java)
    compileOnlyApi(projects.isolation)

    implementation(libs.cloud.paper)
    implementation(libs.adventure.platform.bukkit)

    compileOnlyApi(libs.paper.api)
}

relocate("org.incendo.cloud")
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
