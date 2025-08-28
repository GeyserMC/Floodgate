plugins {
    id("floodgate.base-conventions")
    id("floodgate.shadow-conventions")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

configurations {
    create("includeTransitive").isTransitive = true
    create("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
        isTransitive = false
    }
}

architectury {
    minecraft = libs.versions.minecraft.version.get()
}

loom {
    silentMojangMappingsLicense()
}

indra {
    javaVersions {
        target(21)
    }
}

tasks {
    shadowJar {
        // Mirrors the example fabric project, otherwise tons of dependencies are shaded that shouldn't be
        configurations = listOf(project.configurations.getByName("shadowBundle"))
        // The remapped shadowJar is the final desired mod jar
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("shaded")
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
}
