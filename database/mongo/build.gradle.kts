val mongoClientVersion = "4.4.1"

dependencies {
    provided(projects.core)
    implementation("org.mongodb", "mongodb-driver-sync" , mongoClientVersion)
}

description = "The Floodgate database extension for MongoDB"

relocate("com.mongodb")
relocate("org.bson")