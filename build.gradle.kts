plugins {
    `java-library`
    id("floodgate.build-logic")
    id("io.freefair.lombok") version "8.6" apply false
}

allprojects {
    group = "org.geysermc.floodgate"
    version = property("version")!!
    description = "Allows Bedrock players to join Java edition servers while keeping the server in online mode"
}

val deployProjects: Set<Project> = setOf(
    project(":api"),
    project(":core"),
    project(":bungee"),
    project(":spigot"),
    project(":velocity")
)

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

    if (deployProjects.contains(project)) {
        plugins.apply("floodgate.publish-conventions")
    } else {
        plugins.apply("floodgate.base-conventions")
    }
}
