plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()

    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.architectury.dev/")
}

dependencies {
    implementation(libs.indra.common)
    implementation(libs.indra.git)
    implementation(libs.shadow)
    implementation(libs.gradle.idea.ext)

    implementation("architectury-plugin", "architectury-plugin.gradle.plugin", "3.4-SNAPSHOT")
    implementation("dev.architectury.loom", "dev.architectury.loom.gradle.plugin", "1.1-SNAPSHOT")
    implementation("com.modrinth.minotaur:Minotaur:2.7.5")
}
