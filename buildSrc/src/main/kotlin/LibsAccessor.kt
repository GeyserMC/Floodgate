import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.accessors.dm.LibrariesForLibs

val Project.libs: LibrariesForLibs
    get() = rootProject.extensions.getByType()
