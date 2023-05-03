plugins {
    id("floodgate.shadow-conventions")
    id("io.micronaut.library")
    id("floodgate.dependency-hash")
}

configurations.runtimeClasspath.get()
    .exclude("org.slf4j", "slf4j-api")
    .exclude("javax.validation", "validation-api")
    .exclude("io.micronaut", "micronaut-aop")
    .exclude("io.micronaut", "micronaut-core")
    .exclude("io.micronaut", "micronaut-runtime")
    .exclude("io.micronaut", "micronaut-inject")
    .exclude("io.micronaut", "micronaut-context")

dependencies {
    api(libs.micronaut.hibernate)
    api(libs.micronaut.hikari)
    //runtimeOnly("com.h2database:h2")
}
