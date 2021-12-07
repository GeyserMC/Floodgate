var velocityVersion = "3.0.1"
var log4jVersion = "2.11.2"
var gsonVersion = "2.8.8"
var guavaVersion = "25.1-jre"

dependencies {
    implementation(project(":core"))
    api("cloud.commandframework", "cloud-velocity", Versions.cloudVersion)
    compileOnly("net.kyori", "adventure-api", Versions.adventureApiVersion)
    compileOnly("com.google.code.gson", "gson", gsonVersion)
    compileOnly("com.google.guava", "guava", guavaVersion)
    compileOnly("com.google.inject", "guice", Versions.guiceVersion)
    compileOnly("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
    compileOnly("com.velocitypowered", "velocity-api", velocityVersion)
    compileOnly("org.apache.logging.log4j", "log4j-core", log4jVersion)
    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)
    compileOnly("io.netty", "netty-codec", Versions.nettyVersion)
}

description = "velocity"
