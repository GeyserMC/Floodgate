plugins {
    `java-library`
    id("connect.build-logic")
//    id("com.github.spotbugs") version "4.8.0" apply false
    id("io.freefair.lombok") version "6.3.0" apply false
//    checkstyle
}

allprojects {
    group = "com.minekube.connect"
    version = "0.1.0-SNAPSHOT"
    description =
        "Connects the server/proxy to the global Connect network to reach more players while also supporting online mode server, bungee or velocity mode. Visit https://minekube.com/connect"
}

val platforms = setOf(
    projects.bungee,
    projects.spigot,
    projects.velocity
).map { it.dependencyProject }

//todo re-add pmd and organisation/license/sdcm/issuemanagement stuff

subprojects {
//    apply(plugin = "com.github.spotbugs")

    apply {
        plugin("java-library")
//        plugin("checkstyle")
        plugin("io.freefair.lombok")
        plugin("connect.build-logic")
    }

    when (this) {
        in platforms -> plugins.apply("connect.shadow-conventions")
        else -> plugins.apply("connect.base-conventions")
    }
}