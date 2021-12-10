import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("floodgate.base-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveBaseName.set("floodgate-${project.name}")
        archiveVersion.set("")
        archiveClassifier.set("")
        configureRelocations()
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

fun ShadowJar.configureRelocations() {
    //todo platform-independent relocations
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
}