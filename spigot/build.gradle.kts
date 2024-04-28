var authlibVersion = "1.5.21"
var guavaVersion = "21.0"
var gsonVersion = "2.8.5"

indra {
    javaVersions {
        // For Folia
        target(8)
        minimumToolchain(17)
    }
}

dependencies {
    api(projects.core)

    // TODO move to release once cloud-paper releases for 1.20.5
    // https://repo.papermc.io/#browse/browse:maven-public:org%2Fincendo%2Fcloud-paper%2F2.0.0-SNAPSHOT%2F2.0.0-20240427.220226-58
    implementation("org.incendo", "cloud-paper", "2.0.0-20240427.220226-58")
    // hack to make pre 1.12 work
    implementation("com.google.guava", "guava", guavaVersion)

    compileOnlyApi("dev.folia", "folia-api", Versions.spigotVersion) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
}

relocate("com.google.inject")
relocate("net.kyori")
relocate("org.incendo.cloud")
relocate("io.leangen.geantyref") // used in cloud
// hack to make pre 1.12 work
relocate("com.google.common")
relocate("com.google.guava")
// hack to make (old versions? of) Paper work
relocate("it.unimi")
// since 1.20
relocate("org.yaml")

// these dependencies are already present on the platform
provided("com.mojang", "authlib", authlibVersion)
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)
provided("com.google.code.gson", "gson", gsonVersion)
