import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    java
    id("fabric-loom") version "1.0-SNAPSHOT" apply true
}

dependencies {
    api(projects.isolation) {
        exclude("com.google.guava", "guava")
        exclude("com.google.code.gson", "gson")
        exclude("org.slf4j", "slf4j-api")
        exclude("net.kyori", "*") // Let Adventure-Platform provide its desired Adventure version
        exclude("it.unimi.dsi.fastutil", "*")
    }
    compileOnlyApi(libs.api)

    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc", "fabric-loader", loaderVersion)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabricVersion)

    include(modImplementation("cloud.commandframework", "cloud-fabric", "1.7.0-SNAPSHOT") {
        because("Commands library implementation for Fabric")
    })

    include(modImplementation("net.kyori", "adventure-platform-fabric", "5.4.0-SNAPSHOT") {
        because("Chat library implementation for Fabric that includes methods for communicating with the server")
        // Thanks to zml for this fix
        // The package modifies Brigadier which causes a LinkageError at runtime if included
        exclude("ca.stellardrift", "colonel")
    })
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

tasks {
    jar {
        dependsOn(":fabric-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "floodgate-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val fabricBaseJar = project.projects
            .spigotBase.dependencyProject
            .buildDir
            .resolve("libs")
            .resolve("floodgate-fabric-base.jar")

        from(fabricBaseJar.parentFile) {
            include(fabricBaseJar.name)
            rename("floodgate-fabric-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}