@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // Geyser, Cumulus etc.
        maven("https://repo.opencollab.dev/main")

        // Paper, Velocity
//        maven("https://repo.papermc.io/repository/maven-releases") {
//            mavenContent { releasesOnly() }
//        }
//        maven("https://repo.papermc.io/repository/maven-snapshots") {
//            mavenContent { snapshotsOnly() }
//        }

        maven("https://repo.papermc.io/repository/maven-public")
        // Spigot
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // Spigot, BungeeCord
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        maven("https://libraries.minecraft.net") {
            name = "minecraft"
            mavenContent { releasesOnly() }
        }

        // Fabric
        maven("https://maven.fabricmc.net")

        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }

        mavenLocal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()

        // Fabric, loom specifically
        maven("https://maven.fabricmc.net")
    }

    plugins {
        id("net.kyori.indra")
        id("net.kyori.indra.git")
    }
}

rootProject.name = "floodgate-parent"

include(":api")
include(":core")
include(":universal")
//include(":database")
include(":isolation")

arrayOf("bungee", "spigot", "velocity", "fabric").forEach { platform ->
    arrayOf("base", "isolated").forEach {
        var id = ":$platform-$it"
        // isolated is the new default
        if (id.endsWith("-isolated")) id = ":$platform"

        include(id)
        project(id).projectDir = file("$platform/$it")
    }
}
