import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api("org.geysermc.geyser", "common", Versions.geyserVersion)
    api("org.geysermc.cumulus", "cumulus", Versions.cumulusVersion)
    api("org.geysermc.event", "events", Versions.eventsVersion)

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
