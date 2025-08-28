plugins {
    id("floodgate.modded-conventions")
}

architectury {
    common("neoforge", "fabric")
}

//loom {
//    accessWidenerPath = file("src/main/resources/floodgate.accesswidener")
//    mixin.defaultRefmapName.set("floodgate-refmap.json")
//}

dependencies {
    api(projects.coreNetty4)
    annotationProcessor(projects.coreNetty4)
    annotationProcessor(libs.micronaut.inject.java)
    compileOnlyApi(projects.isolation)

    modApi(libs.adventure.platform.modded)

    compileOnly(libs.mixin)
    compileOnly(libs.asm)

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings.
    compileOnly(libs.fabric.loader)
}
