plugins {
    id("floodgate.base-conventions")
    id("com.gradleup.shadow")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }

    shadowJar {
        archiveBaseName.set("floodgate-${project.name}")
        archiveVersion.set("")
        archiveClassifier.set("")

        dependencies {
            exclude { dep ->
                providedDependencies[project.name]?.any { (name, notation) ->
                    println("Excluding $name, from ${project.name}")
                    dep.name.contains(notation.toString())
                } ?: false
            }
        }

        collectRelocations().forEach { pattern ->
            println("Relocating $pattern for $project")
            relocate(pattern, "org.geysermc.floodgate.shadow.$pattern")
        }

        val destinationDir = System.getenv("DESTINATION_DIRECTORY")
        if (destinationDir != null) {
            destinationDirectory.set(file(destinationDir))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

fun Project.collectRelocations(): Set<String> {
    val result = mutableSetOf<String>()
    relocatedPackages[project.name]?.let { result.addAll(it) }

    configurations.matching { it.name in listOf("api", "implementation") }
        .forEach { config ->
            config.dependencies.withType<ProjectDependency>().forEach { dep ->
                val depProj = project.rootProject.findProject(dep.name)
                if (depProj != null) {
                    result.addAll(depProj.collectRelocations())
                }
            }
        }
    return result
}
