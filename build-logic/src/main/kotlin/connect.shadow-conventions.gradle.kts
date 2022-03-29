import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("connect.base-conventions")
    id("com.github.johnrengelman.shadow")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        archiveBaseName.set("connect-${project.name}")
        archiveVersion.set("")
        archiveClassifier.set("")

        val sJar: ShadowJar = this

        doFirst {
            providedDependencies[project.name]?.forEach { string ->
                sJar.dependencies {
                    println("Excluding $string from ${project.name}")
                    exclude(dependency(string))
                }
            }

            // relocations made in included project dependencies are for whatever reason not
            // forwarded to the project implementing the dependency.
            // (e.g. a relocation in `core` will relocate for core. But when you include `core` in
            // for example Velocity, the relocation will be gone for Velocity)
            addRelocations(project, sJar)
        }
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

fun addRelocations(project: Project, shadowJar: ShadowJar) {
    callAddRelocations(project.configurations.api.get(), shadowJar)
    callAddRelocations(project.configurations.implementation.get(), shadowJar)

    relocatedPackages[project.name]?.forEach { pattern ->
        println("Relocating $pattern for ${shadowJar.project.name}")
        shadowJar.relocate(pattern, "com.minekube.connect.shadow.$pattern")
    }
}

fun callAddRelocations(configuration: Configuration, shadowJar: ShadowJar) =
    configuration.dependencies.forEach {
        if (it is ProjectDependency)
            addRelocations(it.dependencyProject, shadowJar)
    }