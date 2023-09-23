plugins {
    // allow resolution of compileOnlyApi dependencies in Eclipse
    id("eclipse")
}

dependencies {
  provided(projects.core)

  // update HikariCP when we move to Java 11+
  implementation("com.zaxxer", "HikariCP", "4.0.3")

  implementation("com.mysql", "mysql-connector-j", "8.0.32") {
    exclude("com.google.protobuf", "protobuf-java")
  }
}

description = "The Floodgate database extension for MySQL"

// relocate everything from mysql-connector-j and HikariCP
relocate("com.mysql")
relocate("com.zaxxer.hikari")
relocate("org.slf4j")

eclipse {
    classpath {
    	configurations.compileOnlyApi.get().setCanBeResolved(true)
        plusConfigurations.add( configurations.compileOnlyApi.get() )
   	}
}