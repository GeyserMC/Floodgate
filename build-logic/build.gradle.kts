plugins {
    `kotlin-dsl`
    java
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("gradle.plugin.com.github.jengelman.gradle.plugins", "shadow", "7.0.0")
}
