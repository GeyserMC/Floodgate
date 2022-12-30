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
    implementation("gradle.plugin.com.github.johnrengelman", "shadow", "7.1.1")
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext", "gradle-idea-ext", "1.1.7")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
