plugins {
    id("floodgate.publish-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set(archiveBaseName.get() + "-database")
    }
}