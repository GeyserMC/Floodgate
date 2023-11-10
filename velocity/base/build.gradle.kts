var log4jVersion = "2.11.2"
var gsonVersion = "2.8.8"

dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    annotationProcessor(libs.micronaut.inject.java)

    implementation(libs.cloud.velocity)
}

relocate("cloud.commandframework")
relocate("io.leangen.geantyref") // used in cloud

relocate("org.yaml.snakeyaml")

// these dependencies are already present on the platform
provided(libs.gson)
provided(libs.velocity.api)
