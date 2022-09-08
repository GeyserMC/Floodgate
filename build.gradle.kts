plugins {
    `java-library`
    id("connect.build-logic")
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = "com.minekube.connect"
    version = "0.2.0-SNAPSHOT"
    description =
        "Connects the server/proxy to the global Connect network to reach more players while also supporting online mode server, bungee or velocity mode. Visit https://minekube.com/connect"
}

val deployProjects = setOf(
    projects.api,
    // for future Connect integration + Fabric
    projects.core,
    projects.bungee,
    projects.spigot,
    projects.velocity
).map { it.dependencyProject }

//todo re-add checkstyle when we switch back to 2 space indention
// and take a look again at spotbugs someday

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("connect.build-logic")
    }

    when (this) {
        in deployProjects -> plugins.apply("connect.shadow-conventions")
        else -> plugins.apply("connect.base-conventions")
    }
}