plugins {
    `java-library`
    `maven-publish`
//    id("net.ltgt.errorprone")
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
        groupId = rootProject.group as String
        artifactId = project.name
        version = rootProject.version as String
    }
}