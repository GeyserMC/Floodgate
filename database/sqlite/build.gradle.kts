plugins {
    war
}

dependencies {
    providedCompile(projects.core)
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
}

description = "sqlite"
