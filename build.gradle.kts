plugins {
    `java-library`
    id("floodgate.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = "org.geysermc.floodgate"
    version = "2.2.0-SNAPSHOT"
    description = "Allows Bedrock players to join Java edition servers while keeping the server in online mode"
}

val deployProjects = setOf(
    projects.api,
    // for future Floodgate integration + Fabric
    projects.core,
    projects.bungee,
    projects.spigot,
    projects.velocity,
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

    val relativePath = projectDir.relativeTo(rootProject.projectDir).path

    if (relativePath.startsWith("database" + File.separator)) {
        group = rootProject.group as String + ".database"
        plugins.apply("floodgate.database-conventions")
    }

    when (this) {
        in deployProjects -> plugins.apply("floodgate.publish-conventions")
        else -> plugins.apply("floodgate.base-conventions")
    }
}