plugins {
    id("floodgate.generate-templates")
    id("io.micronaut.library") version "3.7.4"
}

dependencies {
    api(projects.api)
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
}

// present on all platforms
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)

relocate("org.bstats")

tasks {
    templateSources {
        replaceToken("floodgateVersion", fullVersion())
        replaceToken("branch", branchName())
        replaceToken("buildNumber", buildNumber())
    }
}
