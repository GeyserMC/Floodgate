import com.google.protobuf.gradle.*

plugins {
    idea // used to let Intellij recognize protobuf generated sources
    id("com.google.protobuf")
}

dependencies {
    api("com.google.code.gson", "gson", Versions.gsonVersion)

    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)
    api("io.grpc", "grpc-protobuf", Versions.gRPCVersion)
    api("build.buf", "connect-kotlin-okhttp", Versions.connectKotlinVersion)
}

relocate("build.buf")

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${Versions.protocVersion}"
    }
    plugins {
        id("connectkt") {
            // source https://github.com/bufbuild/connect-kotlin/releases/download/v0.1.4/protoc-gen-connect-kotlin.jar
            path = file("protoc-gen-connect-kotlin.jar").path
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("connectkt")
            }
        }
    }
}