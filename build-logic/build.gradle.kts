import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.kyori", "indra-common", "2.0.6")
    implementation("org.jfrog.buildinfo", "build-info-extractor-gradle", "4.26.1")
    implementation("gradle.plugin.com.github.johnrengelman", "shadow", "7.1.2")

    // Within the gradle plugin classpath, there is a version conflict between loom and some other
    // plugin for databind. This fixes it: minimum 1.13.2 is required by loom.
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
