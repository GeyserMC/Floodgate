
val minecraftVersion = "1.19"
val loaderVersion = "0.14.6"
val fabricVersion = "0.55.3+1.19"

plugins {
    id("fabric-loom") version "0.12-SNAPSHOT"
    id("java")
    id("maven-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

loom {
    accessWidenerPath.set(file("src/main/resources/floodgate.accesswidener"))
}

repositories {
    // specifically for adventure-platform-fabric:5.4.0-SNAPSHOT
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    api(projects.core) {
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
        exclude("net.kyori", "*") // Let Adventure-Platform provide its desired Adventure version
        exclude("it.unimi.dsi.fastutil", "*")
    }

    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    include(modImplementation("cloud.commandframework:cloud-fabric:1.7.0-SNAPSHOT") {
        because("Commands library implementation for Fabric")
    })

    include(modImplementation("net.kyori:adventure-platform-fabric:5.4.0-SNAPSHOT") {
        because("Chat library implementation for Fabric that includes methods for communicating with the server")
        // Thanks to zml for this fix
        // The package modifies Brigadier which causes a LinkageError at runtime if included
        exclude("ca.stellardrift", "colonel")
    })
}