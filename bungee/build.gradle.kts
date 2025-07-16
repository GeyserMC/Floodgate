var bungeeVersion = "1.21-R0.1-SNAPSHOT"
var gsonVersion = "2.8.0"
var guavaVersion = "21.0"

dependencies {
    api(projects.core)
    implementation("org.incendo", "cloud-bungee", Versions.cloudVersion)
}

relocate("com.google.inject")
relocate("net.kyori")
relocate("org.incendo.cloud")
// used in cloud
relocate("io.leangen.geantyref")
// since 1.20
relocate("org.yaml")

// these dependencies are already present on the platform
provided("net.md-5", "bungeecord-proxy", bungeeVersion)
provided("com.google.code.gson", "gson", gsonVersion)
provided("com.google.guava", "guava", guavaVersion)
