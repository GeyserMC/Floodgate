plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.architectury.dev/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    // Used to access version catalogue from the convention plugins
    // this is OK as long as the same version catalog is used in the main build and build-logic
    // see https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.lombok)
    implementation(libs.micronaut)
    implementation(libs.indra.common)
    implementation(libs.indra.git)
    implementation(libs.indra.licenser.spotless)
    implementation(libs.shadow)
    implementation(libs.gradle.idea.ext)
    implementation(libs.architectury.plugin)
    implementation(libs.architectury.loom)
    //implementation(libs.minotaur) TODO modrinth publishing
}
