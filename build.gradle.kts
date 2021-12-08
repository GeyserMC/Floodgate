import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("floodgate.build-logic") apply false
//    id("com.github.spotbugs") version "4.8.0" apply false

//    id("net.kyori.indra")
//    id("net.kyori.indra.checkstyle")
//    id("net.kyori.indra.license-header")

    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
    id("io.freefair.lombok") version "6.3.0" apply false
}

subprojects {
//    apply(plugin = "pmd")
//    apply(plugin = "com.github.spotbugs")

    apply {
        plugin("java-library")
        plugin("maven-publish")
        plugin("com.github.johnrengelman.shadow")
        plugin("io.freefair.lombok")
        plugin("floodgate.build-logic")
    }

    group = "org.geysermc.floodgate"
    //todo make nicer
    if (project.name in arrayOf("mysql", "sqlite")) {
        group = group as String + ".database"
    }
    version = "2.1.1-SNAPSHOT"

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        compileOnly("org.checkerframework", "checker-qual", Versions.checkerQual)
    }

    tasks {
        val shadowJar = named<ShadowJar>("shadowJar") {
            archiveBaseName.set("floodgate-${project.name}")
            archiveVersion.set("")
            archiveClassifier.set("")
        }
        named("build") {
            dependsOn(shadowJar)
        }

        compileJava {
            options.encoding = Charsets.UTF_8.name()
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}