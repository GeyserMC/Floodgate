import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("floodgate-modded.publish-conventions")
    id("architectury-plugin")
    id("dev.architectury.loom")
    id("com.modrinth.minotaur")
}

// These are all provided by Minecraft/server platforms
provided("com.google.code.gson", "gson")
provided("org.slf4j", ".*")
provided("com.google.guava", "guava")
provided("org.ow2.asm", "asm")
provided("com.nukkitx.fastutil", ".*")

// these we just don't want to include
provided("org.checkerframework", ".*")
provided("com.google.errorprone", ".*")
provided("com.github.spotbugs", "spotbugs-annotations")
provided("com.google.code.findbugs", ".*")

// cloud-fabric/cloud-neoforge jij's all cloud depends already
provided("org.incendo", ".*")
provided("io.leangen.geantyref", "geantyref")

architectury {
    minecraft = libs.versions.minecraft.version.get()
}

loom {
    silentMojangMappingsLicense()
}

configurations {
    create("includeTransitive").isTransitive = true
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    // These are under our own namespace
    shadow(libs.floodgate.api) { isTransitive = false }
    shadow(libs.floodgate.core) { isTransitive = false }

    // Requires relocation
    shadow(libs.bstats) { isTransitive = false }

    // Shadow & relocate these since the (indirectly) depend on quite old dependencies
    shadow(libs.guice) { isTransitive = false }
    shadow(libs.configutils) {
        exclude("org.checkerframework")
        exclude("com.google.errorprone")
        exclude("com.github.spotbugs")
        exclude("com.nukkitx.fastutil")
    }

}

tasks {
    sourcesJar {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    shadowJar {
        // Mirrors the example fabric project, otherwise tons of dependencies are shaded that shouldn't be
        configurations = listOf(project.configurations.shadow.get())

        // Relocate these
        relocate("org.bstats", "org.geysermc.floodgate.shadow.bstats")
        relocate("com.google.inject", "org.geysermc.floodgate.shadow.google.inject")
        relocate("org.yaml", "org.geysermc.floodgate.shadow.org.yaml")

        // The remapped shadowJar is the final desired mod jar
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("shaded")
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    register("remapModrinthJar", RemapJarTask::class) {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
        archiveVersion.set(versionName(project))
        archiveClassifier.set("")
    }

    // Readme sync
    modrinth.get().dependsOn(tasks.modrinthSyncBody)
}

afterEvaluate {
    val providedDependencies = getProvidedDependenciesForProject(project.name)

    // These are shaded, no need to JiJ them
    configurations["shadow"].resolvedConfiguration.resolvedArtifacts.forEach {shadowed ->
        val string = "${shadowed.moduleVersion.id.group}:${shadowed.moduleVersion.id.name}"
        println("Not including shadowed dependency: $string")
        providedDependencies.add(string)
    }

    configurations["includeTransitive"].resolvedConfiguration.resolvedArtifacts.forEach { dep ->
        if (!providedDependencies.contains("${dep.moduleVersion.id.group}:${dep.moduleVersion.id.name}")
            and !providedDependencies.contains("${dep.moduleVersion.id.group}:.*")) {
            println("Including dependency via JiJ: ${dep.id}")
            dependencies.add("include", dep.moduleVersion.id.toString())
        } else {
            println("Not including ${dep.id} for ${project.name}!")
        }
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN")) // Even though this is the default value, apparently this prevents GitHub Actions caching the token?
    projectId.set("bWrNNfkb")
    versionName.set(versionName(project))
    versionNumber.set(projectVersion(project))
    versionType.set("release")
    changelog.set("A changelog can be found at https://github.com/GeyserMC/Floodgate-Modded/commits")

    syncBodyFrom.set(rootProject.file("README.md").readText())

    uploadFile.set(tasks.getByPath("remapModrinthJar"))
    gameVersions.add(libs.minecraft.get().version as String)
    gameVersions.add("1.21.1")
    failSilently.set(false)
}
