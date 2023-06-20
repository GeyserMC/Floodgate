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

        val velocityBaseJar = project.projects
            .velocityBase.dependencyProject
            .buildDir
            .resolve("libs")
            .resolve("floodgate-velocity-base.jar")

        from(velocityBaseJar.parentFile) {
            include(velocityBaseJar.name)
            rename("floodgate-velocity-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}