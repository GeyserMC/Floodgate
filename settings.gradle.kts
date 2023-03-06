@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()

        // Geyser, Cumulus etc.
        maven("https://repo.opencollab.dev/maven-releases") {
            mavenContent { releasesOnly() }
        }
        maven("https://repo.opencollab.dev/maven-snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // Paper, Velocity
//        maven("https://repo.papermc.io/repository/maven-releases") {
//            mavenContent { releasesOnly() }
//        }
//        maven("https://repo.papermc.io/repository/maven-snapshots") {
//            mavenContent { snapshotsOnly() }
//        }
        maven("https://repo.papermc.io/repository/maven-public") {
            content {
                includeGroupByRegex(
                    "(io\\.papermc\\..*|com\\.destroystokyo\\..*|com\\.velocitypowered)"
                )
            }
        }
        // Spigot
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // BungeeCord
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        maven("https://libraries.minecraft.net") {
            name = "minecraft"
            mavenContent { releasesOnly() }
        }

        mavenCentral()

        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("net.kyori.indra")
        id("net.kyori.indra.git")
    }
    includeBuild("build-logic")
}

rootProject.name = "floodgate-parent"

include(":api")
include(":core")
include(":bungee")
include(":spigot")
include(":velocity")
include(":universal")
include(":sqlite")
include(":mysql")
include(":mongo")
project(":sqlite").projectDir = file("database/sqlite")
project(":mysql").projectDir = file("database/mysql")
project(":mongo").projectDir = file("database/mongo")
