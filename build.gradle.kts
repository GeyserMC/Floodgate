plugins {
    `java-library`
    id("floodgate.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
    id("fabric-loom") version "1.0-SNAPSHOT" apply false
}

allprojects {
    group = "org.geysermc.floodgate"
    version = "2.2.0-SNAPSHOT"
    description = "Allows Bedrock players to join Java edition servers while keeping the server in online mode"

    repositories {
//        mavenLocal()

        // Geyser, Cumulus etc.
        maven("https://repo.opencollab.dev/maven-releases") {
            mavenContent { releasesOnly() }
        }
        maven("https://repo.opencollab.dev/maven-snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // Spigot, BungeeCord
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            mavenContent { snapshotsOnly() }
        }

        // Paper, Velocity
        maven("https://repo.papermc.io/repository/maven-public") {
            content {
                includeGroupByRegex(
                    "(io\\.papermc\\..*|com\\.destroystokyo\\..*|com\\.velocitypowered)"
                )
            }
        }

        maven("https://libraries.minecraft.net") {
            name = "minecraft"
            mavenContent { releasesOnly() }
        }

        mavenCentral()

        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
    }
}

val deployProjects = setOf(
    projects.api,
    // for future Floodgate integration + Fabric
    projects.core,
    projects.bungee,
    projects.fabric,
    projects.spigot,
    projects.velocity
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