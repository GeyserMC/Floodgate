plugins {
  id("floodgate.generate-templates")
}

provided(libs.bungee)
provided(libs.folia.api)
provided(libs.velocity.api)

// todo use an isolated class loader in the future
provided(libs.gson)

tasks {
  templateSources {
    replaceToken("branch", branchName())
  }
}
