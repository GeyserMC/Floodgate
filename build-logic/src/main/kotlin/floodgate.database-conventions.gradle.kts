import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("floodgate.shadow-conventions")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set(archiveBaseName.get() + "-database")
}
