import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider

val providedDependencies = mutableMapOf<String, MutableSet<String>>()

fun Project.provided(pattern: String, name: String, excludedOn: Int = 0b110) {
    providedDependencies.getOrPut(project.name) { mutableSetOf() }
        .add("${calcExclusion(pattern, 0b100, excludedOn)}:${calcExclusion(name, 0b10, excludedOn)}")
}

fun Project.provided(dependency: ProjectDependency) =
    provided(dependency.group!!, dependency.name)

fun Project.provided(dependency: MinimalExternalModuleDependency) =
    provided(dependency.module.group, dependency.module.name)

fun Project.provided(provider: Provider<MinimalExternalModuleDependency>) =
    provided(provider.get())

fun getProvidedDependenciesForProject(projectName: String): MutableSet<String> {
    return providedDependencies.getOrDefault(projectName, emptySet()).toMutableSet()
}

private fun calcExclusion(section: String, bit: Int, excludedOn: Int): String =
    if (excludedOn and bit > 0) section else ""

fun projectVersion(project: Project): String =
    project.version.toString().replace("SNAPSHOT", "b" + buildNumber())

fun versionName(project: Project): String =
    "Floodgate-" + project.name.replaceFirstChar { it.uppercase() } + "-" + projectVersion(project)

fun buildNumber(): Int =
    (System.getenv("GITHUB_RUN_NUMBER"))?.let { Integer.parseInt(it) } ?: -1
