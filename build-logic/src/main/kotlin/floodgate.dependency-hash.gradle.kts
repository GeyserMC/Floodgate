import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

plugins {
    `java-library`
}

tasks.register("listDependencyInfo") {
    doLast {
        listDependencies().forEach {
            println(it)
        }
    }
}

tasks.register("writeDependencyInfo") {
    doLast {
        Files.writeString(
            dependenciesInfoFile(),
            dependenciesToString(listDependencies())
        )
    }
}

tasks.register("checkDependencyInfoFile") {
    doLast {
        val asString = dependenciesToString(listDependencies())
        if (!asString.contentEquals(Files.readString(dependenciesInfoFile()))) {
            throw IllegalStateException("The dependency hashes file is outdated!")
        }
        println("The dependency hashes file is up-to-date")
    }
}

tasks.named("build") {
    dependsOn("checkDependencyHashesFile")
}

fun dependenciesToString(dependencies: Collection<Dependency>): String {
    val builder = StringBuilder()
    dependencies.forEach {
        if (builder.length > 1) {
            builder.append("\n")
        }
        builder.append(it)
    }
    return builder.toString()
}

fun dependenciesInfoFile(): Path {
    return project.projectDir.toPath().resolve("src/main/resources/dependencyInfo.txt")
}

fun listDependencies(): Collection<Dependency> {
    return listConfigurationDependencies(configurations.default.get())
}

fun listConfigurationDependencies(configuration: Configuration): Collection<Dependency> {
    val deps = configuration.resolvedConfiguration.firstLevelModuleDependencies
        .associateBy({"${it.moduleGroup}:${it.moduleName}"}, {it})

    return configuration.resolvedConfiguration.resolvedArtifacts
        .mapNotNull {
            val id = it.id.componentIdentifier
            if (id is ModuleComponentIdentifier && deps.containsKey("${id.group}:${id.module}")) {
                convertDependency(id, it.file)
            } else {
                null
            }
        }
}

fun convertDependency(id: ModuleComponentIdentifier, file: File): Dependency {
    return Dependency(id.group, id.module, id.version, calculateSha256(file))
}

fun calculateSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return Base64.getEncoder().encodeToString(digest.digest(file.readBytes()))
}

data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val sha256: String
) {
    override fun toString(): String {
        return arrayOf(groupId, artifactId, version, sha256).joinToString(":")
    }
}