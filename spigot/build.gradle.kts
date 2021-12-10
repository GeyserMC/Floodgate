var authlibVersion = "1.5.21"
var guavaVersion = "21.0"
var gsonVersion = "2.8.5"

dependencies {
    api(project(":core"))

    implementation("com.google.guava", "guava", guavaVersion)
    implementation("cloud.commandframework", "cloud-bukkit", Versions.cloudVersion)
    implementation("net.kyori", "adventure-text-serializer-legacy", Versions.adventureApiVersion)
    implementation("net.kyori", "adventure-text-serializer-gson", Versions.adventureApiVersion)

    compileOnly("org.spigotmc", "spigot-api", Versions.spigotVersion)
    compileOnly("com.mojang", "authlib", authlibVersion)
    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)
    compileOnly("io.netty", "netty-codec", Versions.nettyVersion)
    compileOnly("com.google.code.gson", "gson", gsonVersion)
    compileOnly("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
}

description = "spigot"
