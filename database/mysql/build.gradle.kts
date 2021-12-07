plugins {
    war
}

dependencies {
    providedCompile(projects.core)
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.4")
}

description = "mysql"
