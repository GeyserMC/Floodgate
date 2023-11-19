architectury {
    common("fabric")
}

loom {
    accessWidenerPath.set(file("src/main/resources/floodgate.accesswidener"))
}

dependencies {
    api(projects.core)

    compileOnly(libs.mixin)
    annotationProcessor(projects.core)
    annotationProcessor(libs.micronaut.inject.java)
    compileOnlyApi(projects.isolation)
}

provided(libs.gson)