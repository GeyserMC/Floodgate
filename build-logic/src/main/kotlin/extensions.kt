/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

import net.kyori.indra.git.IndraGitExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.the

fun Project.fullVersion(): String {
    var version = version.toString()
    if (version.endsWith("-SNAPSHOT")) {
        version += " (b${buildNumberAsString()}-${lastCommitHash()})"
    }
    return version
}

fun Project.lastCommitHash(): String? =
    the<IndraGitExtension>().commit().orNull?.name?.substring(0, 7)

fun Project.branchName(): String =
    the<IndraGitExtension>().branchName().orNull ?: jenkinsBranchName() ?: "local/dev"

fun Project.shouldAddBranchName(): Boolean =
    System.getenv("IGNORE_BRANCH")?.toBoolean() ?: (branchName() !in arrayOf("master", "local/dev"))

fun Project.versionWithBranchName(): String =
    branchName().replace(Regex("[^0-9A-Za-z-_]"), "-") + '-' + version

fun buildNumber(): Int =
    (System.getenv("BUILD_NUMBER"))?.let { Integer.parseInt(it) } ?: -1

fun buildNumberAsString(): String =
    buildNumber().takeIf { it != -1 }?.toString() ?: "??"

val providedDependencies = mutableMapOf<String, MutableSet<Pair<String, Any>>>()
val relocatedPackages = mutableMapOf<String, MutableSet<String>>()

fun Project.provided(dependency: String, includeTransitiveDeps: Boolean = true) {
    val lastColonIndex = dependency.lastIndexOf(':')
    val groupPrefix = dependency.substring(0, lastColonIndex + 1)

    providedDependencies.getOrPut(project.name) { mutableSetOf() }.add(groupPrefix to groupPrefix)
    dependencies.add("compileOnlyApi", dependency) {
        isTransitive = includeTransitiveDeps
    }
}

fun Project.provided(dependency: ProjectDependency) {
    providedDependencies.getOrPut(project.name) { mutableSetOf() }
        .add(dependency.group + ":" + dependency.name to dependency)
    dependencies.add("compileOnlyApi", dependency)
}

fun Project.relocate(pattern: String) =
    relocatedPackages.getOrPut(project.name) { mutableSetOf() }.add(pattern)

// todo remove these when we're not using Jenkins anymore
private fun jenkinsBranchName(): String? = System.getenv("BRANCH_NAME")
