plugins {
    `java-library`
    id("floodgate.build-logic") apply false
//    id("com.github.spotbugs") version "4.8.0" apply false
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects{
    group = "org.geysermc.floodgate"
    version = "2.1.1-SNAPSHOT"
}

val platforms = setOf(
    projects.bungee,
    projects.spigot,
    projects.velocity
).map { it.dependencyProject }

val api: Project = projects.api.dependencyProject

subprojects {
//    apply(plugin = "pmd")
//    apply(plugin = "com.github.spotbugs")

    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("floodgate.build-logic")
    }

    if (project.projectDir.startsWith("database/")) {
        group = rootProject.group as String + ".database"
    }

    when (this) {
        in platforms -> plugins.apply("floodgate.shadow-conventions")
        api -> plugins.apply("floodgate.base-conventions")
        else -> plugins.apply("floodgate.standard-conventions")
    }

    dependencies {
        compileOnly("org.checkerframework", "checker-qual", Versions.checkerQual)
    }
}