@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.opencollab.dev/main/")
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }

        maven("https://maven.architectury.dev/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.fabricmc.net/")
    }
    plugins {
        id("net.kyori.indra")
        id("net.kyori.indra.git")
    }
}

rootProject.name = "floodgate-parent"

include(":api")
include(":universal")
include(":isolation")
include(":mod")

arrayOf("common", "netty4").forEach {
    val id = ":core-$it"
    include(id)
    project(id).projectDir = file("core/$it")
}

arrayOf("bungee", "spigot", "velocity", "fabric").forEach { platform ->
    arrayOf("base", "isolated").forEach {
        var id = ":$platform-$it"
        // isolated is the new default
        if (id.endsWith("-isolated")) id = ":$platform"

        include(id)
        project(id).projectDir = file("$platform/$it")
    }
}
