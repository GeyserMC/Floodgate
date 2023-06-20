plugins {
    java
    libs.plugins.loom
}

dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    compileOnlyApi(projects.isolation)

    compileOnlyApi(libs.fabric.api)
    compileOnlyApi(libs.fabric.loader)





    //implementation(libs.cloud.fabric)

    //minecraft("com.mojang", "minecraft", minecraftVersion)
    //mappings(loom.officialMojangMappings())

    //modImplementation("net.fabricmc", "fabric-loader", loaderVersion)
    //modImplementation("net.fabricmc.fabric-api", "fabric-api", fabricVersion)

    //include(modImplementation("cloud.commandframework", "cloud-fabric", "1.7.0-SNAPSHOT") {
    //    because("Commands library implementation for Fabric")
    //})

    //include(modImplementation("net.kyori", "adventure-platform-fabric", "5.4.0-SNAPSHOT") {
    //    because("Chat library implementation for Fabric that includes methods for communicating with the server")
        // Thanks to zml for this fix
        // The package modifies Brigadier which causes a LinkageError at runtime if included
    //    exclude("ca.stellardrift", "colonel")
    //})
}

/*
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
*/
