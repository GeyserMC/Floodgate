@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {

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

        // Forge
        maven("https://maven.minecraftforge.net/")

        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
        mavenCentral()
        mavenLocal()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()

        // Fabric, loom specifically
        maven("https://maven.fabricmc.net")

        // Forge
        maven("https://maven.minecraftforge.net/")
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

arrayOf("bungee", "spigot", "velocity").forEach { platform ->
    arrayOf("base", "isolated").forEach {
        var id = ":$platform-$it"
        // isolated is the new default
        if (id.endsWith("-isolated")) id = ":$platform"

        include(id)
        project(id).projectDir = file("$platform/$it")
    }
}

arrayOf("common", "fabric").forEach { platform ->
    arrayOf("base", "isolated").forEach {
        var id = ":mod:$platform-$it"
        // isolated is the new default
        if (id.endsWith("-isolated")) id = ":$platform"
        println("Including mod project $id")
        include(id)
        project(id).projectDir = file("mod/$platform/$it")
    }
}
