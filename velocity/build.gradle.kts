var velocityVersion = "3.2.0-SNAPSHOT"
var log4jVersion = "2.11.2"
var gsonVersion = "2.8.8"
var guavaVersion = "25.1-jre"

indra {
    javaVersions {
        // For Velocity API
        target(11)
    }
}

dependencies {
    api(projects.core)
    implementation("org.incendo", "cloud-velocity", Versions.cloudVersion)
}

relocate("org.incendo.cloud")
// used in cloud
relocate("io.leangen.geantyref")


// these dependencies are already present on the platform
provided("com.google.code.gson", "gson", gsonVersion)
provided("com.google.guava", "guava", guavaVersion)
provided("com.google.inject", "guice", Versions.guiceVersion)
provided("org.yaml", "snakeyaml", Versions.snakeyamlVersion) // included in Configurate
provided("com.velocitypowered", "velocity-api", velocityVersion)
provided("org.apache.logging.log4j", "log4j-core", log4jVersion)
