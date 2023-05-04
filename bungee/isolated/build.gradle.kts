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

        val bungeeBaseJar = project.projects
            .bungeeBase.dependencyProject
            .buildDir
            .resolve("libs")
            .resolve("floodgate-bungee-base.jar")

        from(bungeeBaseJar.parentFile) {
            include(bungeeBaseJar.name)
            rename("floodgate-bungee-base.jar", "platform-base.jar")
            into("bundled/")
        }
    }
}