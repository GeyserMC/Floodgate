plugins {
    `kotlin-dsl`
    java
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("gradle.plugin.com.github.jengelman.gradle.plugins", "shadow", "7.0.0")
    implementation("org.jfrog.buildinfo", "build-info-extractor-gradle", "4.25.2")
}
