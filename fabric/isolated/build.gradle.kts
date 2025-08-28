plugins {
    id("floodgate.modded-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

dependencies {
    // FIXME why does it break when this is set to api scope???
    compileOnlyApi(projects.isolation)

    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)
    include(libs.cloud.fabric)
    include(libs.fabric.permissions.api)
}

tasks {
    jar {
        dependsOn(":fabric-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName = "floodgate-${project.name}"
        archiveVersion = ""
        archiveClassifier = ""

        val libsDir = project.projects
            .fabricBase.dependencyProject
            .layout.buildDirectory.dir("libs")

        from(libsDir) {
            include("floodgate-fabric-base.jar")
            rename("floodgate-fabric-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}
