plugins {
    id("floodgate.generate-templates")
    id("floodgate.dependency-hash")
    id("io.micronaut.library") version "3.7.4"
}

dependencies {
    api(projects.api)
    api("org.geysermc.configutils", "configutils", Versions.configUtilsVersion)

    api("com.google.inject", "guice", Versions.guiceVersion)
    api("com.nukkitx.fastutil", "fastutil-short-object-maps", Versions.fastutilVersion)
    api("com.nukkitx.fastutil", "fastutil-int-object-maps", Versions.fastutilVersion)
    api("org.java-websocket", "Java-WebSocket", Versions.javaWebsocketVersion)
    api("cloud.commandframework", "cloud-core", Versions.cloudVersion)
    api("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
    api("org.bstats", "bstats-base", Versions.bstatsVersion)

    api("com.google.guava:guava:31.1-jre")

    annotationProcessor("io.micronaut:micronaut-inject-java")
    api("io.micronaut", "micronaut-inject-java")
    api("io.micronaut", "micronaut-context")

    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    runtimeOnly("com.h2database:h2")

//    annotationProcessor("io.micronaut.data:micronaut-data-document-processor")
//    compileOnly("io.micronaut.data:micronaut-data-mongodb")
//    runtimeOnly("org.mongodb:mongodb-driver-sync")
}

// present on all platforms
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)

relocate("org.bstats")

tasks {
    templateSources {
        replaceToken("floodgateVersion", fullVersion())
        replaceToken("branch", branchName())
        replaceToken("buildNumber", buildNumber())
    }
}
