enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        gradlePluginPortal()
    }
    plugins {
        id("net.kyori.blossom") version "1.2.0"
    }
    includeBuild("build-logic")
}

rootProject.name = "floodgate-parent"

include(":api")
include(":ap")
include(":core")
include(":bungee")
include(":fabric")
include(":spigot")
include(":velocity")
include(":sqlite")
include(":mysql")
include(":mongo")
project(":sqlite").projectDir = file("database/sqlite")
project(":mysql").projectDir = file("database/mysql")
project(":mongo").projectDir = file("database/mongo")
