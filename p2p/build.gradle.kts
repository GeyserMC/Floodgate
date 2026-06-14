java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(projects.core)

    implementation("io.libp2p:jvm-libp2p:${Versions.jvmLibp2pVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlinStdlibVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
