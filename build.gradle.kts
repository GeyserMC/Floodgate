plugins {
    `java-library`
    id("floodgate.build-logic")
    id("io.freefair.lombok") version "8.0.1" apply false
    id("io.micronaut.library") version "3.7.8" apply false
}

allprojects {
    group = "org.geysermc.floodgate"
    version = property("version")!!
    description = "Allows Bedrock players to join Java edition servers while keeping the server in online mode"
}

val deployProjects = setOf(
    projects.api,
    // for future Floodgate integration + Fabric
    projects.core,
    projects.bungee,
    projects.spigot,
    projects.velocityIsolated,
    projects.universal
).map { it.dependencyProject }

val shadowProjects = setOf(
    projects.api,
    // for future Floodgate integration + Fabric
    projects.core,
    projects.bungee,
    projects.spigot,
    projects.velocityBase,
    projects.universal
).map { it.dependencyProject }

//todo re-add checkstyle when we switch back to 2 space indention
// and take a look again at spotbugs someday

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("floodgate.build-logic")
    }

    when (this) {
        in deployProjects -> plugins.apply("floodgate.publish-conventions")
        else -> plugins.apply("floodgate.base-conventions")
    }

    if (this in shadowProjects) {
        plugins.apply("floodgate.shadow-conventions")
    }
}