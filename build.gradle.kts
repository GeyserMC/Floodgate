plugins {
    `java-library`
    alias(libs.plugins.micronaut) apply false
    alias(libs.plugins.lombok) apply false

    // TODO: plugin("fabric-loom") version "1.0-SNAPSHOT" apply false
}

allprojects {
    group = "org.geysermc.floodgate"
    description = "Allows Bedrock players to join Java edition servers while keeping the server in online mode"

    apply {
        plugin("floodgate.build-logic")
        plugin("net.kyori.indra.git")
    }

    if (shouldAddBranchName()) {
        version = versionWithBranchName()
    }
}

//todo differentiate maven publishing from downloads publishing
val deployProjects = setOf(
    projects.api,
    // for future Floodgate integration + Fabric
    projects.core,
    projects.database,
    projects.isolation,
    projects.bungee,
    projects.fabric,
    projects.spigot,
    projects.velocity,
    projects.bungeeBase,
    projects.spigotBase,
    projects.velocityBase,
    projects.universal
).map { it.dependencyProject }

val shadowProjects = setOf(
    projects.api,
    // for future Floodgate integration + Fabric
    projects.core,
    projects.bungeeBase,
    projects.spigotBase,
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