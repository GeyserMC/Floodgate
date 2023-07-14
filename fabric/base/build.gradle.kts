plugins {
    java
    id("fabric-loom") version (libs.versions.loom) apply true
}

dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    compileOnlyApi(projects.isolation)

    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.loader)

    // Commands library implementation for Fabric
    //modImplementation(libs.cloud.fabric) {
    //    because("Commands library implementation for Fabric")
    //}

    minecraft(libs.fabric.minecraft)
    mappings(loom.officialMojangMappings())

    loom {
        accessWidenerPath.set(file("src/main/resources/floodgate.accesswidener"))
    }

    // TODO: Relocations, provided, fixing dependencies for cloud and adventure

    /*
    include(modImplementation("net.kyori", "adventure-platform-fabric", "5.4.0-SNAPSHOT") {
        because("Chat library implementation for Fabric that includes methods for communicating with the server")
        // Thanks to zml for this fix
        // The package modifies Brigadier which causes a LinkageError at runtime if included
        exclude("ca.stellardrift", "colonel")
    })
    */
}