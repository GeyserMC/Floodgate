import com.google.protobuf.gradle.*
import net.kyori.blossom.BlossomExtension

plugins {
    idea // used to let Intellij recognize protobuf generated sources
    id("net.kyori.blossom")
    id("com.google.protobuf")
}

dependencies {
    api(projects.api)
    api("org.geysermc.configutils", "configutils", Versions.configUtilsVersion)

    api("com.google.inject", "guice", Versions.guiceVersion)
    api("cloud.commandframework", "cloud-core", Versions.cloudVersion)
    api("org.bstats", "bstats-base", Versions.bstatsVersion)

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    runtimeOnly("io.grpc", "grpc-netty-shaded", Versions.gRPCVersion)
    implementation("io.grpc", "grpc-protobuf", Versions.gRPCVersion)
    implementation("io.grpc", "grpc-stub", Versions.gRPCVersion)
    implementation("javax.annotation", "javax.annotation-api", "1.3.2")

    // Test deps — pinned to versions still compatible with the Java 8 source target.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.5")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("io.netty", "netty-transport", Versions.nettyVersion)
    testImplementation("io.netty", "netty-codec", Versions.nettyVersion)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// present on all platforms
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)
provided("io.netty", "netty-transport-native-unix-common", Versions.nettyVersion)

relocate("org.bstats")

configure<BlossomExtension> {
    val constantsFile = "src/main/java/com/minekube/connect/util/Constants.java"
    replaceToken("\${connectVersion}", fullVersion(), constantsFile)
    replaceToken("\${branch}", branchName(), constantsFile)
    replaceToken("\${buildNumber}", buildNumber(), constantsFile)
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${Versions.protocVersion}"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.gRPCVersion}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}
