import com.google.protobuf.gradle.*

plugins {
    idea // used to let Intellij recognize protobuf generated sources
    id("com.google.protobuf")
}

dependencies {
    api("com.google.code.gson", "gson", Versions.gsonVersion)

    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)

    implementation("com.squareup.okhttp3", "okhttp", Versions.okHttpVersion)
    implementation("build.buf", "connect-kotlin-okhttp", Versions.connectKotlinVersion)
    implementation("build.buf", "connect-kotlin-google-java-ext", Versions.connectKotlinVersion)
    implementation("io.grpc", "grpc-protobuf", Versions.gRPCVersion)
}

relocate("build.buf")
relocate("com.squareup.okhttp3")


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
        id("connectkt") {
            // source https://github.com/bufbuild/connect-kotlin/releases/download/v0.1.4/protoc-gen-connect-kotlin.jar
            path = file("protoc-gen-connect-kotlin.jar").path
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
//                id("grpc") // Use ConnectKt instead of standard gRPC
                id("connectkt")
            }
        }
    }
}
