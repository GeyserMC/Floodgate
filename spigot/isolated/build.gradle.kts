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

        val libsDir = project.projects
            .spigotBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("floodgate-spigot-base.jar")
            rename("floodgate-spigot-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}