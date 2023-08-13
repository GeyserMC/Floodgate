plugins {
    java
}

dependencies {
    api(projects.isolation)
    compileOnlyApi(libs.paper.api)
}

tasks {
    jar {
        dependsOn(":spigot-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "floodgate-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val spigotBaseJar = project.projects
            .spigotBase.dependencyProject
            .buildDir
            .resolve("libs")
            .resolve("floodgate-spigot-base.jar")

        from(spigotBaseJar.parentFile) {
            include(spigotBaseJar.name)
            rename("floodgate-spigot-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}