plugins {
    `java-library`
    `maven-publish`
//    id("net.ltgt.errorprone")
    id("net.kyori.indra.git")
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", Versions.checkerQual)
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
            expand(
                "id" to "connect",
                "name" to "connect",
                "version" to fullVersion(),
                "description" to project.description,
                "url" to "https://minekube.com",
                "author" to "Minekube"
            )
        }
    }
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
}
