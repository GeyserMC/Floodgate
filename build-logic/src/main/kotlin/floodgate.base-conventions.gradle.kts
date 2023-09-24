plugins {
    `java-library`
//    id("net.ltgt.errorprone")
    id("net.kyori.indra")
    id("net.kyori.indra.git")
    // allow resolution of compileOnlyApi dependencies in Eclipse
    id("eclipse")
}

dependencies {
    compileOnly("org.checkerframework", "checker-qual", Versions.checkerQual)
}

indra {
    github("GeyserMC", "Floodgate") {
        ci(true)
        issues(true)
        scm(true)
    }
    mitLicense()

    javaVersions {
        // without toolchain & strictVersion sun.misc.Unsafe won't be found
        minimumToolchain(8)
        strictVersions(true)
    }
}

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

eclipse {
    classpath {
    	configurations.compileOnlyApi.get().setCanBeResolved(true)
        plusConfigurations.add( configurations.compileOnlyApi.get() )
   	}
}