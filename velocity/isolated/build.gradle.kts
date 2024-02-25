plugins {
    java
}

dependencies {
    api(projects.isolation)
    compileOnlyApi(libs.velocity.api)
}

tasks {
    jar {
        dependsOn(":velocity-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "floodgate-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val libsDir = project.projects
            .velocityBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("floodgate-velocity-base.jar")
            rename("floodgate-velocity-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}