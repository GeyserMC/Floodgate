plugins {
    id("floodgate.generate-templates")
    id("floodgate.dependency-hash")
    id("io.micronaut.library")
}

dependencies {
    api(projects.api)
    compileOnlyApi(projects.isolation)
    api("org.geysermc.configutils", "configutils", Versions.configUtilsVersion)

    api("com.google.inject", "guice", Versions.guiceVersion)
    api("com.nukkitx.fastutil", "fastutil-short-object-maps", Versions.fastutilVersion)
    api("com.nukkitx.fastutil", "fastutil-int-object-maps", Versions.fastutilVersion)
    api("org.java-websocket", "Java-WebSocket", Versions.javaWebsocketVersion)
    api("cloud.commandframework", "cloud-core", Versions.cloudVersion)
    api("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
    api("org.bstats", "bstats-base", Versions.bstatsVersion)

    api("com.google.guava:guava:31.1-jre")

    annotationProcessor("io.micronaut:micronaut-inject-java")
    api("io.micronaut", "micronaut-inject-java")
    api("io.micronaut", "micronaut-context")
    api("io.micronaut", "micronaut-http-client")
    api("io.micronaut", "micronaut-validation")

    //todo add hibernate dependency back in core,
    // it's not possible to make it optional as the service files would be messed up
    api(projects.database)

    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    implementation("io.micronaut.data:micronaut-data-model")
    implementation("jakarta.persistence:jakarta.persistence-api:2.2.3")
}

// present on all platforms
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)

relocate("org.bstats")

tasks {
    templateSources {
        replaceToken("fullVersion", fullVersion())
        replaceToken("version", version)
        replaceToken("branch", branchName())
        replaceToken("buildNumber", buildNumber())
    }
}
