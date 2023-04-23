var log4jVersion = "2.11.2"
var gsonVersion = "2.8.8"
var guavaVersion = "25.1-jre"

plugins {
    java
}

dependencies {
    api(projects.isolation)
}

tasks {
    jar {
        dependsOn(":velocity-base:build", configurations.runtimeClasspath)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from (configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

        archiveBaseName.set("floodgate-${project.name}")
        archiveVersion.set("")
        archiveClassifier.set("")

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