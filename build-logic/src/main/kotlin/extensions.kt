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
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.the
import java.io.ByteArrayOutputStream

/**
 * Calculates the version from git tags.
 * - For tagged commits: returns the tag (e.g., "0.7.0")
 * - For non-tagged commits: returns "{last-tag}-SNAPSHOT" (e.g., "0.7.0-SNAPSHOT")
 * - For dirty working tree: appends "-dirty"
 */
fun Project.gitVersion(): String {
    // Check if GITHUB_REF_TYPE is "tag" - if so, use the tag name directly
    val refType = System.getenv("GITHUB_REF_TYPE")
    val refName = System.getenv("GITHUB_REF_NAME")
    if (refType == "tag" && !refName.isNullOrEmpty()) {
        return refName.removePrefix("v")
    }

    // Try to get version from git describe
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--tags", "--always", "--dirty")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val describe = stdout.toString().trim()

        if (describe.isEmpty()) {
            "0.0.0-SNAPSHOT"
        } else {
            parseGitDescribe(describe)
        }
    } catch (e: Exception) {
        "0.0.0-SNAPSHOT"
    }
}

/**
 * Parses git describe output into a version string.
 * Examples:
 * - "0.7.0" -> "0.7.0" (exactly on tag)
 * - "v0.7.0" -> "0.7.0" (exactly on tag with v prefix)
 * - "0.7.0-5-gabcdef" -> "0.7.0-SNAPSHOT" (5 commits after tag)
 * - "0.7.0-5-gabcdef-dirty" -> "0.7.0-SNAPSHOT-dirty" (with uncommitted changes)
 * - "abcdef" -> "0.0.0-SNAPSHOT" (no tags, just commit hash)
 * - "abcdef-dirty" -> "0.0.0-SNAPSHOT-dirty"
 */
private fun parseGitDescribe(describe: String): String {
    val isDirty = describe.endsWith("-dirty")
    val cleanDescribe = describe.removeSuffix("-dirty")

    // Pattern: tag-commits-hash or just tag or just hash
    val parts = cleanDescribe.split("-")

    val version = when {
        // Just a hash (no tags exist)
        parts.size == 1 && parts[0].matches(Regex("[a-f0-9]+")) -> "0.0.0-SNAPSHOT"
        // Exactly on a tag: "0.7.0" or "v0.7.0"
        parts.size == 1 -> parts[0].removePrefix("v")
        // Tag with v prefix split: "v0", "7", "0" - rejoin
        parts[0] == "v" || parts[0].startsWith("v") -> {
            // Check if this looks like a versioned tag with commits after
            if (parts.size >= 3 && parts[parts.size - 1].startsWith("g")) {
                // Has commits after tag: "v0.7.0-5-gabcdef" or "0.7.0-5-gabcdef"
                val tagParts = parts.dropLast(2) // Remove commit count and hash
                tagParts.joinToString("-").removePrefix("v") + "-SNAPSHOT"
            } else {
                // Just the tag, possibly with dashes in it
                cleanDescribe.removePrefix("v")
            }
        }
        // Tag-commits-hash format: "0.7.0-5-gabcdef"
        parts.size >= 3 && parts[parts.size - 1].startsWith("g") -> {
            val tagParts = parts.dropLast(2)
            tagParts.joinToString("-").removePrefix("v") + "-SNAPSHOT"
        }
        // Some other format, use as-is
        else -> cleanDescribe.removePrefix("v")
    }

    return if (isDirty) "$version-dirty" else version
}

fun Project.isSnapshot(): Boolean =
    version.toString().contains("-SNAPSHOT")

fun Project.fullVersion(): String {
    val ver = version.toString()
    return if (ver.contains("-SNAPSHOT")) {
        val commitHash = lastCommitHash() ?: "unknown"
        ver.replace("-SNAPSHOT", "-SNAPSHOT+$commitHash")
    } else {
        ver
    }
}

fun Project.lastCommitHash(): String? =
    the<IndraGitExtension>().commit()?.name?.substring(0, 7)

// retrieved from https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project
// some properties might be specific to Jenkins
fun Project.branchName(): String =
    System.getenv("GIT_BRANCH") ?: System.getenv("GITHUB_REF_NAME") ?: "local/dev"
fun Project.buildNumber(): Int =
    Integer.parseInt(System.getenv("BUILD_NUMBER") ?: System.getenv("GITHUB_RUN_NUMBER") ?: "-1")

fun Project.buildNumberAsString(): String =
    buildNumber().takeIf { it != -1 }?.toString() ?: "??"

val providedDependencies = mutableMapOf<String, MutableSet<Pair<String, Any>>>()
val relocatedPackages = mutableMapOf<String, MutableSet<String>>()

fun Project.provided(pattern: String, name: String, version: String, excludedOn: Int = 0b110, includeTransitiveDeps: Boolean = true) {
    val format = "${calcExclusion(pattern, 0b100, excludedOn)}:" +
            "${calcExclusion(name, 0b10, excludedOn)}:" +
            calcExclusion(version, 0b1, excludedOn)

    providedDependencies.getOrPut(project.name) { mutableSetOf() }.add(Pair(format, format))
    val dep = dependencies.add("compileOnlyApi", "$pattern:$name:$version")
    if (dep is ExternalModuleDependency) {
        dep.isTransitive = includeTransitiveDeps
    }
}

fun Project.provided(dependency: ProjectDependency) {
    providedDependencies.getOrPut(project.name) { mutableSetOf() }
        .add(Pair(dependency.group + ":" + dependency.name, dependency))
    dependencies.add("compileOnlyApi", dependency)
}


fun Project.relocate(pattern: String) =
    relocatedPackages.getOrPut(project.name) { mutableSetOf() }
        .add(pattern)

private fun calcExclusion(section: String, bit: Int, excludedOn: Int): String =
    if (excludedOn and bit > 0) section else ""
