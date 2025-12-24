import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.jvm.tasks.Jar
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("floodgate.base-conventions")
    id("com.gradleup.shadow")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveBaseName.set("floodgate-${project.name}")
        archiveVersion.set("")
        archiveClassifier.set("")

        val sJar: ShadowJar = this

        doFirst {
            providedDependencies[project.name]?.forEach { (name, notation) ->
                sJar.dependencies {
                    println("Excluding $name from ${project.name}")
                    exclude(dependency(notation))
                }
            }

            // relocations made in included project dependencies are for whatever reason not
            // forwarded to the project implementing the dependency.
            // (e.g. a relocation in `core` will relocate for core. But when you include `core` in
            // for example Velocity, the relocation will be gone for Velocity)
            addRelocations(project, sJar)
        }

        val destinationDir = System.getenv("DESTINATION_DIRECTORY");
        if (destinationDir != null) {
            destinationDirectory.set(file(destinationDir))
        }
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

fun addRelocations(project: Project, shadowJar: ShadowJar) {
    callAddRelocations(project, project.configurations.api.get(), shadowJar)
    callAddRelocations(project, project.configurations.implementation.get(), shadowJar)

    relocatedPackages[project.name]?.forEach { pattern ->
        println("Relocating $pattern for ${shadowJar.project.name}")
        shadowJar.relocate(pattern, "org.geysermc.floodgate.shadow.$pattern")
    }
}

fun callAddRelocations(owner: Project, configuration: Configuration, shadowJar: ShadowJar) {
    configuration.allDependencies.forEach { dep ->
        if (dep is ProjectDependency) {
            val child = owner.findProject(dep.path)
                ?: error("Project '${dep.path}' not found (from ${owner.path})")
            addRelocations(child, shadowJar)
        }
    }
}
