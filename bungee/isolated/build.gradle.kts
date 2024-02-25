plugins {
    java
}

dependencies {
    api(projects.isolation)
    compileOnlyApi(libs.bungee)
}

tasks {
    jar {
        dependsOn(":bungee-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "floodgate-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val libsDir = project.projects
            .bungeeBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("floodgate-bungee-base.jar")
            rename("floodgate-bungee-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}