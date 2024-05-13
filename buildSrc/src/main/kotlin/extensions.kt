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
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the

fun Project.fullVersion(): String {
    var version = version.toString()
    if (version.endsWith("-SNAPSHOT")) {
        version += " (b${buildNumberAsString()}-${lastCommitHash()})"
    }
    return version
}

fun Project.lastCommitHash(): String? =
    the<IndraGitExtension>().commit()?.name?.substring(0, 7)

fun Project.branchName(): String =
    the<IndraGitExtension>().branchName() ?: "local/dev"

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

fun Project.providedDependency(dependency: MinimalExternalModuleDependency) {
    val format = "${dependency.group}:${dependency.name}:"
    providedDependencies.getOrPut(project.name) { mutableSetOf() }.add(Pair(format, format))
}

fun Project.providedDependency(provider: Provider<MinimalExternalModuleDependency>) =
    providedDependency(provider.get())

fun Project.provided(dependency: MinimalExternalModuleDependency) {
    providedDependency(dependency)
    dependencies.add("compileOnlyApi",
        "${dependency.group}:${dependency.name}:${dependency.versionConstraint.requiredVersion}"
    )
}

fun Project.provided(provider: Provider<MinimalExternalModuleDependency>) =
    provided(provider.get())

fun Project.relocate(pattern: String) =
    relocatedPackages.getOrPut(project.name) { mutableSetOf() }
        .add(pattern)
