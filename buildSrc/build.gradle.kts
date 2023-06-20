plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.indra.common)
    implementation(libs.indra.git)
    implementation(libs.shadow)
    implementation(libs.gradle.idea.ext)
}
