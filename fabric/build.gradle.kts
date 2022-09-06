
val minecraftVersion = "1.19.2"
val loaderVersion = "0.17.5-beta.1"
val quiltedFabricVersion = "4.0.0-beta.11+0.60.0"

plugins {
    id("org.quiltmc.loom")
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
    api(projects.core)

    minecraft("com.mojang", "minecraft", minecraftVersion)
    mappings(loom.officialMojangMappings())

    modImplementation("org.quiltmc:quilt-loader:${loaderVersion}")
    modImplementation("org.quiltmc.quilted-fabric-api:quilted-fabric-api:${quiltedFabricVersion}-${minecraftVersion}")
    modImplementation("org.quiltmc.quilted-fabric-api:quilted-fabric-api-deprecated:${quiltedFabricVersion}-${minecraftVersion}")
}