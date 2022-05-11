plugins {
    id("floodgate.shadow-conventions")
}

tasks {
    shadowJar {
        archiveBaseName.set(archiveBaseName.get() + "-database")
    }
}