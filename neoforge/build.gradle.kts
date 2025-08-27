architectury {
    platformSetupLoomIde()
    neoForge()
}

provided("com.google.guava", "failureaccess")

// Used to extend runtime/compile classpaths
val common: Configuration by configurations.creating
// Needed to read mixin config in the runServer task, and for the architectury transformer
// (e.g. the @ExpectPlatform annotation)
val developmentNeoForge: Configuration = configurations.getByName("developmentNeoForge")
// Our custom transitive include configuration
val includeTransitive: Configuration = configurations.getByName("includeTransitive")

configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentNeoForge.extendsFrom(configurations["common"])
}

dependencies {
    // See https://github.com/google/guava/issues/6618
    modules {
        module("com.google.guava:listenablefuture") {
            replacedBy("com.google.guava:guava", "listenablefuture is part of guava")
        }
    }

    neoForge(libs.neoforge)
    // "namedElements" configuration should be used to depend on different loom projects
    common(project(":mod", configuration = "namedElements")) { isTransitive = false }
    // Bundle transformed classes of the common module for production mod jar
    shadow(project(path = ":mod", configuration = "transformProductionNeoForge")) { isTransitive = false }

    includeTransitive(libs.floodgate.core)

    implementation(libs.floodgate.core)
    implementation(libs.guice)

    modImplementation(libs.cloud.neoforge)
    include(libs.cloud.neoforge)
}

tasks {
    processResources {
        from(project(":mod").file("src/main/resources/floodgate.accesswidener")) {
            into("/assets/")
        }
    }

    remapJar {
        dependsOn(processResources)
        atAccessWideners.add("floodgate.accesswidener")
        archiveBaseName.set("floodgate-neoforge")
    }

    modrinth {
        loaders.add("neoforge")
    }
}
