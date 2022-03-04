plugins {
    `java-library`
    id("floodgate.build-logic")
//    id("com.github.spotbugs") version "4.8.0" apply false
    id("io.freefair.lombok") version "6.3.0" apply false
}

allprojects {
    group = "org.geysermc.floodgate"
    version = "2.1.1-SNAPSHOT"
    description =
        "Allows Bedrock players to join Java edition servers while keeping the server in online mode"
}

val platforms = setOf(
    projects.bungee,
    projects.spigot,
    projects.velocity
).map { it.dependencyProject }

//todo re-add pmd and organisation/license/sdcm/issuemanagement stuff

val api: Project = projects.api.dependencyProject

subprojects {
//    apply(plugin = "pmd")
//    apply(plugin = "com.github.spotbugs")

    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("floodgate.build-logic")
    }

    when (this) {
        in platforms -> plugins.apply("floodgate.shadow-conventions")
        api -> plugins.apply("floodgate.shadow-conventions")
        else -> plugins.apply("floodgate.base-conventions")
    }
}