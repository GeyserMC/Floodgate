plugins {
    `java-library`
    `maven-publish`
//    id("net.ltgt.errorprone")
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", Versions.checkerQual)
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
            expand(
                "id" to "floodgate",
                "name" to "floodgate",
                "version" to project.version,
                "description" to project.description,
                "url" to "https://geysermc.org",
                "author" to "GeyserMC"
            )
        }
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withSourcesJar()
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = project.group as String
        artifactId = "floodgate-" + project.name
        version = project.version as String
    }
}