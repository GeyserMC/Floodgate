import net.kyori.blossom.BlossomExtension

plugins {
    id("net.kyori.blossom")
}

dependencies {
    api(projects.api)
    api("org.geysermc", "api", "3.0.0-SNAPSHOT")
//    api("org.geysermc", "floodgate-legacy-api", "3.0.0-SNAPSHOT")
    api("org.geysermc.configutils", "configutils", Versions.configUtilsVersion)

    compileOnly(projects.ap)
    annotationProcessor(projects.ap)

    api("com.google.inject", "guice", Versions.guiceVersion)
    api("com.nukkitx.fastutil", "fastutil-short-object-maps", Versions.fastutilVersion)
    api("com.nukkitx.fastutil", "fastutil-int-object-maps", Versions.fastutilVersion)
    api("org.java-websocket", "Java-WebSocket", Versions.javaWebsocketVersion)
    api("cloud.commandframework", "cloud-core", Versions.cloudVersion)
    api("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
    api("org.bstats", "bstats-base", Versions.bstatsVersion)
}

// present on all platforms
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)

relocate("org.bstats")

configure<BlossomExtension> {
    val constantsFile = "src/main/java/org/geysermc/floodgate/util/Constants.java"
    replaceToken("\${floodgateVersion}", fullVersion(), constantsFile)
    replaceToken("\${branch}", branchName(), constantsFile)
    replaceToken("\${buildNumber}", buildNumber(), constantsFile)
}
