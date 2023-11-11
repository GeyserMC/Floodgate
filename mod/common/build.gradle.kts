architectury {
    common("fabric-base", "fabric")
}

dependencies {
    api(projects.core)
    annotationProcessor(projects.core)
    annotationProcessor(libs.micronaut.inject.java)
}