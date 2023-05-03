var authlibVersion = "1.5.21"
var guavaVersion = "21.0"
var gsonVersion = "2.8.5"

dependencies {
    api(projects.core)
    annotationProcessor(projects.core)

    implementation(projects.isolation)

    implementation("cloud.commandframework", "cloud-bukkit", Versions.cloudVersion)
    // hack to make pre 1.12 work
    implementation("com.google.guava", "guava", guavaVersion)

    compileOnlyApi("dev.folia", "folia-api", Versions.spigotVersion)
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
provided("com.mojang", "authlib", authlibVersion)
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)
provided("com.google.code.gson", "gson", gsonVersion)
provided("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
