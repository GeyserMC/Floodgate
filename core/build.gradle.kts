dependencies {
    api(project(":api", "shadow"))
    api("com.google.inject", "guice", Versions.guiceVersion)
    api("com.nukkitx.fastutil", "fastutil-short-object-maps", Versions.fastutilVersion)
    api("com.nukkitx.fastutil", "fastutil-int-object-maps", Versions.fastutilVersion)
    api("org.java-websocket", "Java-WebSocket", Versions.javaWebsocketVersion)
    api("net.kyori", "adventure-api", Versions.adventureApiVersion)
    api("cloud.commandframework", "cloud-core", Versions.cloudVersion)
    api("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)
    compileOnly("io.netty", "netty-codec", Versions.nettyVersion)
}

description = "core"
