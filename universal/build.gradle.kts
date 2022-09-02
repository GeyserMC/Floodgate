import net.kyori.blossom.BlossomExtension

plugins {
  id("net.kyori.blossom")
}

provided("com.github.SpigotMC.BungeeCord", "bungeecord-proxy", Versions.bungeeCommit)
provided("com.destroystokyo.paper", "paper-api", Versions.spigotVersion)
provided("com.velocitypowered", "velocity-api", Versions.velocityVersion)

// todo use an isolated class loader in the future
provided("com.google.code.gson", "gson", "2.8.5")


configure<BlossomExtension> {
  val constantsFile = "src/main/java/org/geysermc/floodgate/universal/util/Constants.java"
  replaceToken("\${branch}", branchName(), constantsFile)
}
