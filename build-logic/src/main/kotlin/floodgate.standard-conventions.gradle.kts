plugins {
    id("floodgate.base-conventions")
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        from(components["java"])
    }
}