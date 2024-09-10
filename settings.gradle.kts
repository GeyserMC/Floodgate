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
        id("net.kyori.blossom")
        id("net.kyori.indra")
        id("net.kyori.indra.git")
        id("floodgate-modded.build-logic")
    }

    includeBuild("build-logic")
}

rootProject.name = "floodgate-modded"

include(":mod")
include(":fabric")
include(":neoforge")
