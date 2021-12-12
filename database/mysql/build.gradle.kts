val mariadbClientVersion = "2.7.4"

dependencies {
    provided(projects.core)
    implementation("org.mariadb.jdbc", "mariadb-java-client" , mariadbClientVersion)
}

description = "The Floodgate database extension for MySQL"

relocate("org.mariadb")
