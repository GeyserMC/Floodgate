import org.gradle.plugins.ide.eclipse.model.AccessRule
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry

plugins {
    // suppress unsafe access errors for eclipse
    id("eclipse")
}

var bungeeCommit = "master-SNAPSHOT"
var gsonVersion = "2.8.0"
var guavaVersion = "21.0"

dependencies {
    api(projects.core)
    implementation("cloud.commandframework", "cloud-bungee", Versions.cloudVersion)
}

relocate("com.google.inject")
relocate("net.kyori")
relocate("cloud.commandframework")
// used in cloud
relocate("io.leangen.geantyref")
// since 1.20
relocate("org.yaml")

// these dependencies are already present on the platform
provided("com.github.SpigotMC.BungeeCord", "bungeecord-proxy", bungeeCommit)
provided("com.google.code.gson", "gson", gsonVersion)
provided("com.google.guava", "guava", guavaVersion)

// found how to do this here https://github.com/JFormDesigner/markdown-writer-fx/blob/main/build.gradle.kts
eclipse {
	classpath {
		file {
			whenMerged.add( object: Action<org.gradle.plugins.ide.eclipse.model.Classpath> {
				override fun execute( classpath: org.gradle.plugins.ide.eclipse.model.Classpath ) {
					val jre = classpath.entries.find {
						it is AbstractClasspathEntry &&
							it.path.contains("org.eclipse.jdt.launching.JRE_CONTAINER")
					} as AbstractClasspathEntry

					// make sun.misc accessible in Eclipse project
					// (when refreshing Gradle project in buildship)
					jre.accessRules.add(AccessRule("accessible", "sun/misc/**"))

					// remove trailing slash from jre path
					if (jre.path.endsWith("/")) jre.path = jre.path.substring(0, jre.path.length - 1)
				}
			} )
		}
	}
}