var bungeeCommit = "bda1605";
var gsonVersion = "2.8.0";
var guavaVersion = "21.0";

dependencies {
    api(project(":core"))
    implementation("cloud.commandframework", "cloud-bungee", "1.5.0")
    implementation("net.kyori", "adventure-text-serializer-gson", Versions.adventureApiVersion)
    implementation("net.kyori", "adventure-text-serializer-bungeecord", Versions.adventurePlatformVersion)
    compileOnly("com.github.SpigotMC.BungeeCord", "bungeecord-proxy", bungeeCommit)
    compileOnly("com.google.code.gson", "gson", gsonVersion)
    compileOnly("com.google.guava", "guava", guavaVersion)
    compileOnly("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
}

description = "bungee"
