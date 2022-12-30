plugins {
  id("floodgate.generate-templates")
}

provided("com.github.SpigotMC.BungeeCord", "bungeecord-proxy", Versions.bungeeCommit)
provided("com.destroystokyo.paper", "paper-api", Versions.spigotVersion)
provided("com.velocitypowered", "velocity-api", Versions.velocityVersion)

// todo use an isolated class loader in the future
provided("com.google.code.gson", "gson", "2.8.5")

tasks {
  templateSources {
    replaceToken("branch", branchName())
  }
}
