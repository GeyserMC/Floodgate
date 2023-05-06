import net.kyori.blossom.BlossomExtension

plugins {
    id("net.kyori.blossom")
}

dependencies {
    api(projects.api)

    api("org.geysermc.configutils", "configutils", Versions.configUtilsVersion)

    api("com.google.inject", "guice", Versions.guiceVersion)
    api("cloud.commandframework", "cloud-core", Versions.cloudVersion)
    api("org.yaml", "snakeyaml", Versions.snakeyamlVersion)
    api("org.bstats", "bstats-base", Versions.bstatsVersion)

    implementation("javax.annotation", "javax.annotation-api", "1.3.2")

    implementation("com.squareup.okhttp3", "okhttp", Versions.okHttpVersion)
    implementation("build.buf", "connect-kotlin-okhttp", Versions.connectKotlinVersion)
    implementation("build.buf", "connect-kotlin-google-java-ext", Versions.connectKotlinVersion)
    implementation("io.grpc", "grpc-protobuf", Versions.gRPCVersion)
}

relocate("org.bstats")
relocate("build.buf")
relocate("com.squareup.okhttp3")


// present on all platforms
provided("io.netty", "netty-transport", Versions.nettyVersion)
provided("io.netty", "netty-codec", Versions.nettyVersion)
provided("io.netty", "netty-transport-native-unix-common", Versions.nettyVersion)

configure<BlossomExtension> {
    val constantsFile = "src/main/java/com/minekube/connect/util/Constants.java"
    replaceToken("\${connectVersion}", fullVersion(), constantsFile)
    replaceToken("\${branch}", branchName(), constantsFile)
    replaceToken("\${buildNumber}", buildNumber(), constantsFile)
}