dependencies {
    api("org.geysermc", "common", Versions.geyserVersion)
    api("org.geysermc.cumulus", "cumulus", Versions.cumulusVersion)
    api("org.geysermc.event", "events", Versions.eventsVersion)

    compileOnly("io.netty", "netty-transport", Versions.nettyVersion)
}
