plugins {
    id("floodgate.base-conventions")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

architectury {
    minecraft = libs.versions.minecraft.version.get()
}

loom {
    silentMojangMappingsLicense()
}

indra {
    javaVersions {
        target(21)
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
}
