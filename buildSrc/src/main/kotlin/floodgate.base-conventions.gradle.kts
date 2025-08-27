plugins {
    `java-library`
//    id("net.ltgt.errorprone")
    id("net.kyori.indra")
    id("net.kyori.indra.git")
//    id("net.kyori.indra.licenser.spotless")
    id("floodgate.depsize")
    id("io.freefair.lombok")
}

val rootProperties: Map<String, *> = project.rootProject.properties
group = rootProperties["group"] as String + "." + rootProperties["id"] as String
version = if (shouldAddBranchName()) versionWithBranchName() else rootProperties["version"] as String
description = rootProperties["description"] as String

dependencies {
    compileOnly("org.checkerframework", "checker-qual", "3.19.0")
}

indra {
    github("GeyserMC", "Floodgate") {
        ci(true)
        issues(true)
        scm(true)
    }
    mitLicense()

    javaVersions {
        target(17)
    }

}

//spotless {
//    java {
//        //palantirJavaFormat()
//        formatAnnotations()
//    }
//    ratchetFrom("origin/development")
//}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
            expand(
                "id" to "floodgate",
                "name" to "floodgate",
                "version" to fullVersion(),
                "description" to project.description,
                "url" to "https://geysermc.org",
                "author" to "GeyserMC"
            )
        }
    }
}