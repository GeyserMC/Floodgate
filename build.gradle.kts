plugins {
    `java-library`
    id("floodgate.build-logic")
//    id("com.github.spotbugs") version "4.8.0" apply false
    id("io.freefair.lombok") version "6.3.0" apply false
//    checkstyle
}

allprojects {
    group = "org.geysermc.floodgate"
    version = "2.1.1-SNAPSHOT"
    description = "Allows Bedrock players to join Java edition servers while keeping the server in online mode"
}

val platforms = setOf(
    projects.bungee,
    projects.spigot,
    projects.velocity
).map { it.dependencyProject }

projects.api.dependencyProject.plugins.apply("floodgate.publish-conventions")

//todo re-add pmd and organisation/license/sdcm/issuemanagement stuff

subprojects {
//    apply(plugin = "com.github.spotbugs")

    apply {
        plugin("java-library")
//        plugin("checkstyle")
        plugin("io.freefair.lombok")
        plugin("floodgate.build-logic")
    }

//    checkstyle {
//        toolVersion = "9.3"
//        configFile = rootProject.projectDir.resolve("checkstyle.xml")
//        maxErrors = 0
//        maxWarnings = 0
//    }

    val relativePath = projectDir.relativeTo(rootProject.projectDir).path

    if (relativePath.startsWith("database" + File.separator)) {
        group = rootProject.group as String + ".database"
        plugins.apply("floodgate.database-conventions")
    } else {
        when (this) {
            in platforms -> plugins.apply("floodgate.publish-conventions")
            else -> plugins.apply("floodgate.base-conventions")
        }
    }
}