plugins {
    `java-library`
    id("architectury-plugin") apply true
    id("dev.architectury.loom") apply true
}

architectury {
    minecraft = "1.20.2"
}

loom {
    silentMojangMappingsLicense()
    mixin.defaultRefmapName.set("floodgate-mod-refmap.json")
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.2")
    mappings(loom.officialMojangMappings())
}

repositories {
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.architectury.dev/")
}