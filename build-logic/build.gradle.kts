plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.kyori:indra-common:4.0.0")
    implementation("net.kyori:indra-git:4.0.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")
    implementation("gradle.plugin.org.jetbrains.gradle.plugin.idea-ext:gradle-idea-ext:1.1.7")
}
