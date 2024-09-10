plugins {
    `java-library`
    id("floodgate-modded.build-logic")
    alias(libs.plugins.lombok) apply false
}

val platforms = setOf(
    projects.fabric,
    projects.neoforge,
    projects.mod
).map { it.dependencyProject }

subprojects {
    apply {
        plugin("java-library")
        plugin("io.freefair.lombok")
        plugin("floodgate-modded.build-logic")
    }

    when (this) {
        in platforms -> plugins.apply("floodgate-modded.platform-conventions")
        else -> plugins.apply("floodgate-modded.base-conventions")
    }
}