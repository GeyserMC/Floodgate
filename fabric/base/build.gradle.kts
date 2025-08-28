plugins {
    id("floodgate.modded-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

// Used to extend runtime/compile classpaths
val common: Configuration by configurations.creating
// Needed to read mixin config in the runServer task, and for the architectury transformer
// (e.g. the @ExpectPlatform annotation)
val developmentFabric: Configuration = configurations.getByName("developmentFabric")
//// Our custom transitive include configuration
//val includeTransitive: Configuration = configurations.getByName("includeTransitive")

configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentFabric.extendsFrom(configurations["common"])
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)
    // "namedElements" configuration should be used to depend on different loom projects
    common(project(":mod", configuration = "namedElements"))
    // Bundle transformed classes of the common module for production mod jar
    shadowBundle(project(path = ":mod", configuration = "transformProductionFabric"))
    modImplementation(libs.cloud.fabric)

    compileOnlyApi(projects.isolation)
}
