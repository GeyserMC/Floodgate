plugins {
    // allow resolution of compileOnlyApi dependencies in Eclipse
    id("eclipse")
}

val mongoClientVersion = "4.4.1"

dependencies {
    provided(projects.core)
    implementation("org.mongodb", "mongodb-driver-sync" , mongoClientVersion)
}

description = "The Floodgate database extension for MongoDB"

relocate("com.mongodb")
relocate("org.bson")

eclipse {
    classpath {
    	configurations.compileOnlyApi.get().setCanBeResolved(true)
        plusConfigurations.add( configurations.compileOnlyApi.get() )
   	}
}