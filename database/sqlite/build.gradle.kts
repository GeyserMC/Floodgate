plugins {
    // allow resolution of compileOnlyApi dependencies in Eclipse
    id("eclipse")
}

val sqliteJdbcVersion = "3.36.0.3"

dependencies {
    provided(projects.core)
    implementation("org.xerial", "sqlite-jdbc", sqliteJdbcVersion)
}

description = "The Floodgate database extension for SQLite"

eclipse {
    classpath {
    	configurations.compileOnlyApi.get().setCanBeResolved(true)
        plusConfigurations.add( configurations.compileOnlyApi.get() )
   	}
}