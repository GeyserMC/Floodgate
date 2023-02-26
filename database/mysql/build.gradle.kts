plugins {
    // allow resolution of compileOnlyApi dependencies in Eclipse
    id("eclipse")
}

val mysqlClientVersion = "8.0.30"
val hikariVersion = "4.0.3"

dependencies {
    provided(projects.core)
    implementation("mysql", "mysql-connector-java", mysqlClientVersion)
    implementation("com.zaxxer", "HikariCP", hikariVersion)
}

description = "The Floodgate database extension for MySQL"

relocate("org.mariadb")

eclipse {
    classpath {
    	configurations.compileOnlyApi.get().setCanBeResolved(true)
        plusConfigurations.add( configurations.compileOnlyApi.get() )
   	}
}