plugins {
    application
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(project(":mod:common-base").file("src/main/resources/floodgate.accesswidener"))
}

dependencies {
    api(projects.mod.commonBase)

    modApi(libs.fabric.api)
    modImplementation(libs.fabric.loader)

    // Commands library implementation for Fabric
    modImplementation(libs.cloud.fabric) {
        because("Commands library implementation for Fabric")
    }

    modImplementation(libs.kyori.adventure) {
        because("Chat library implementation for Fabric that includes methods for communicating with the server")
        // Thanks to zml for this fix
        // The package modifies Brigadier which causes a LinkageError at runtime if included
        exclude("ca.stellardrift", "colonel")
    }
}

// using loom requires us to re-define all repositories here, lol
repositories {
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
}