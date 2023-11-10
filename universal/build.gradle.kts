plugins {
  id("floodgate.generate-templates")
}

provided(libs.bungee)
provided(libs.paper.api)
provided(libs.velocity.api)
provided(libs.fabric.loader)
provided(libs.fabric.api)

// todo use an isolated class loader in the future
provided(libs.gson)

tasks {
  templateSources {
    replaceToken("branch", branchName())
  }
}
