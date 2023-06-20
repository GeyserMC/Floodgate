plugins {
    java
    id("fabric-loom") version "1.2-SNAPSHOT" apply true
}

dependencies {
    //api(projects.core)
    annotationProcessor(projects.core)
    //compileOnlyApi(projects.isolation)

    compileOnlyApi(libs.fabric.api)
    compileOnlyApi(libs.fabric.loader)

    // modImplementation(libs.cloud.fabric)
    minecraft(libs.fabric.minecraft)
    mappings(loom.officialMojangMappings())

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
