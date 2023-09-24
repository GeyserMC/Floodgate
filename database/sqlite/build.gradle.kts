val sqliteJdbcVersion = "3.36.0.3"

dependencies {
    provided(projects.core)
    implementation("org.xerial", "sqlite-jdbc", sqliteJdbcVersion)
}

description = "The Floodgate database extension for SQLite"