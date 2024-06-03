import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api("com.google.code.gson", "gson", Versions.gsonVersion)

    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("")
    }
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("shaded")
    }
}