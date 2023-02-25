plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.kyori", "indra-common", "3.0.1")
    implementation("net.kyori", "indra-git", "3.0.1")
    implementation("gradle.plugin.com.github.johnrengelman", "shadow", "7.1.1")
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext", "gradle-idea-ext", "1.1.7")
}
