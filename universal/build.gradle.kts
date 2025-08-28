plugins {
  id("floodgate.publish-conventions")
  id("floodgate.generate-templates")
  id("floodgate.shadow-conventions")
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
