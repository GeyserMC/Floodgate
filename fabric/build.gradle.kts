import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

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
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.release.set(17)
}

loom {
    accessWidenerPath.set(file("src/main/resources/floodgate.accesswidener"))
}

// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
// if it is present.
// If you remove this task, sources will not be generated.
//val sourcesJar = tasks.create<Jar>("sourcesJar") {
    //archiveClassifier.set("sources")
    //from(sourceSets["main"].allSource)
//}

// This is easier than what is immediately above?
java {
    withSourcesJar()
}

// fixme: this should exist apparently
//val shadowJar by tasks.getting(ShadowJar::class) {
    //configurations = listOf(
        //project.configurations.shadow.fileCollection()
    //)
//}

val remappedShadowJar = tasks.create<RemapJarTask>("remappedShadowJar") {
    dependsOn(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile) //fixme: deprecated
    addNestedDependencies.set(true)
    archiveFileName.set("floodgate-fabric.jar")
}

tasks.assemble {
    dependsOn(remappedShadowJar)
}

artifacts {
    archives(remappedShadowJar)
    shadow(tasks.shadowJar)
}

repositories {
    // specifically for adventure-platform-fabric:5.4.0-SNAPSHOT
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
}

// todo: perform exclusions using floodgate build logic
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
