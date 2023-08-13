plugins {
  id("floodgate.generate-templates")
}

provided(libs.bungee)
provided(libs.paper.api)
provided(libs.velocity.api)

// todo use an isolated class loader in the future
provided(libs.gson)

tasks {
  templateSources {
    replaceToken("branch", branchName())
  }
}
