var authlibVersion = "1.5.21"
var guavaVersion = "31.1-jre"
var gsonVersion = "2.8.5"

dependencies {
    api(projects.core)

    implementation("cloud.commandframework", "cloud-bukkit", Versions.cloudVersion)
    // hack to make pre 1.12 work
    implementation("com.google.guava", "guava", guavaVersion)
}

relocate("com.google.inject")
relocate("com.google.protobuf")
relocate("net.kyori")
relocate("cloud.commandframework")
relocate("io.leangen.geantyref") // used in cloud
// hack to make pre 1.12 work
relocate("com.google.common")
relocate("com.google.guava")
// hack to make (old versions? of) Paper work
relocate("it.unimi")

// these dependencies are already present on the platform
provided("com.destroystokyo.paper", "paper-api", Versions.spigotVersion)
provided("com.mojang", "authlib", authlibVersion)
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)
provided("com.google.code.gson", "gson", gsonVersion)
provided("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
provided("com.viaversion", "viaversion-bukkit", Versions.viaVersionVersion)
