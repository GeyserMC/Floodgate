var bungeeCommit = "bda1605"
var gsonVersion = "2.8.0"
var guavaVersion = "21.0"

dependencies {
    api(projects.core)
    implementation("cloud.commandframework", "cloud-bungee", Versions.cloudVersion)
}

relocate("com.google.inject")
relocate("com.google.protobuf")
relocate("net.kyori")
relocate("cloud.commandframework")
// used in cloud
relocate("io.leangen.geantyref")

// these dependencies are already present on the platform
provided("com.github.SpigotMC.BungeeCord", "bungeecord-proxy", bungeeCommit)
provided("com.google.code.gson", "gson", gsonVersion)
provided("com.google.guava", "guava", guavaVersion)
provided("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
