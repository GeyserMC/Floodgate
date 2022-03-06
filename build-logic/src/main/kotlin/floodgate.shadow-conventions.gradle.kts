import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("floodgate.base-conventions")
    id("com.github.johnrengelman.shadow")
    id("com.jfrog.artifactory")
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
            providedDependencies[project.name]?.forEach { string ->
                sJar.dependencies {
                    println("Excluding $string from ${project.name}")
                    exclude(dependency(string))
                }
            }
        }
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = project.group as String
        artifactId = "floodgate-" + project.name
        version = project.version as String

        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
}

artifactory {
    publish {
        repository {
            setRepoKey(if (isSnapshot()) "maven-snapshots" else "maven-releases")
            setMavenCompatible(true)
        }
        defaults {
            publishConfigs("archives")
            setPublishArtifacts(true)
            setPublishPom(true)
            setPublishIvy(false)
        }
    }
}