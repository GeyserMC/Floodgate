architectury {
    common("neoforge", "fabric")
}

loom {
    accessWidenerPath = file("src/main/resources/floodgate.accesswidener")
    mixin.defaultRefmapName.set("floodgate-refmap.json")
}

dependencies {
    api(libs.floodgate.core)
    api(libs.floodgate.api)
    api(libs.guice)

    compileOnly(libs.mixin)
    compileOnly(libs.asm)
    modCompileOnly(libs.geyser.mod) { isTransitive = false }
    modCompileOnly(libs.geyser.core) { isTransitive = false }

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings.
    compileOnly(libs.fabric.loader)
}

afterEvaluate {
    // We don't need these
    tasks.named("remapModrinthJar").configure {
        enabled = false
    }

    tasks.named("modrinth").configure {
        enabled = false
    }
}