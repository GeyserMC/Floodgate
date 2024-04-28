import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("java")
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
}

loom {
    accessWidenerPath = file("src/main/resources/floodgate.accesswidener")
}

dependencies {
    //to change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:1.20.5")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.15.10")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.97.6+1.20.5")

    // Base Floodgate
    implementation("org.geysermc.floodgate:core:2.2.3-SNAPSHOT")
    shadow("org.geysermc.floodgate:core:2.2.3-SNAPSHOT") { isTransitive = false }
    shadow("org.geysermc.floodgate:api:2.2.3-SNAPSHOT") { isTransitive = false }

    // Requires relocation
    shadow("org.bstats:bstats-base:3.0.2")

    // Shadow & relocate these since the (indirectly) depend on quite old dependencies
    shadow("com.google.inject:guice:6.0.0") { isTransitive = false }
    shadow("org.geysermc.configutils:configutils:1.0-SNAPSHOT") {
        exclude("org.checkerframework")
        exclude("com.google.errorprone")
        exclude("com.github.spotbugs")
        exclude("com.nukkitx.fastutil")
    }

    include("aopalliance:aopalliance:1.0")
    include("javax.inject:javax.inject:1")
    include("jakarta.inject:jakarta.inject-api:2.0.1")
    include("org.java-websocket:Java-WebSocket:1.5.2")

    // Just like Geyser, include these
    include("org.geysermc.geyser", "common", "2.2.3-SNAPSHOT")
    include("org.geysermc.cumulus", "cumulus", "1.1.2")
    include("org.geysermc.event", "events", "1.1-SNAPSHOT")
    include("org.lanternpowered", "lmbda", "2.0.0") // used in events

    // cloud
    include("org.incendo:cloud-fabric:2.0.0-SNAPSHOT")
    modImplementation("org.incendo:cloud-fabric:2.0.0-SNAPSHOT")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}

repositories {
    //mavenLocal()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.opencollab.dev/main/")
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com.github.*")
        }
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

java {
    withSourcesJar()
}

tasks {
    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        relocate("org.bstats", "org.geysermc.floodgate.shadow.bstats")
        relocate("com.google.inject", "org.geysermc.floodgate.shadow.google.inject")
        relocate("org.yaml", "org.geysermc.floodgate.shadow.org.yaml")
    }

    processResources {
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    remapJar {
        dependsOn(shadowJar)
        mustRunAfter(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        addNestedDependencies = true // todo?
        archiveFileName.set("floodgate-fabric.jar")
    }

    register("remapModrinthJar", RemapJarTask::class) {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        addNestedDependencies = true
        archiveVersion.set(project.version.toString() + "+build."  + System.getenv("GITHUB_RUN_NUMBER"))
        archiveClassifier.set("")
    }
}

publishing {
    publications {
        register("publish", MavenPublication::class) {
            from(project.components["java"])

            // skip shadow jar from publishing. Workaround for https://github.com/johnrengelman/shadow/issues/651
            val javaComponent = project.components["java"] as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
        }
    }

    repositories {
        mavenLocal()
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN")) // Prevent GitHub Actions from caching empty Modrinth token
    projectId.set("bWrNNfkb")
    versionNumber.set(project.version as String + "-" + System.getenv("GITHUB_RUN_NUMBER"))
    versionType.set("beta")
    changelog.set("A changelog can be found at https://github.com/GeyserMC/Floodgate-Fabric/commits")

    syncBodyFrom.set(rootProject.file("README.md").readText())

    uploadFile.set(tasks.named("remapModrinthJar"))
    gameVersions.addAll("1.20.5")

    loaders.add("fabric")

    dependencies {
        required.project("fabric-api")
    }
}
