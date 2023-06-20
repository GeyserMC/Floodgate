var log4jVersion = "2.11.2"
var gsonVersion = "2.8.8"
var guavaVersion = "25.1-jre"

dependencies {
    api(projects.core)
    annotationProcessor(projects.core)

    implementation(libs.cloud.velocity)
}

relocate("cloud.commandframework")
// used in cloud
relocate("io.leangen.geantyref")

relocate("org.yaml.snakeyaml")


// these dependencies are already present on the platform
provided(libs.gson)
provided(libs.guava)
provided(libs.velocity.api)
