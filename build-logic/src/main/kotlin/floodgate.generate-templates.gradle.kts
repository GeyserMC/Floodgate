import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig
import javax.inject.Inject
import org.gradle.api.tasks.Copy

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
}

registerGenerateTemplateTasks()

fun Project.registerGenerateTemplateTasks() {
    // main and test
    extensions.getByType<SourceSetContainer>().all {
        val javaDestination = layout.buildDirectory.dir("generated/sources/templates/$name")
        val javaSrcDir = layout.projectDirectory.dir("src/$name/templates")
        val javaGenerateTask = tasks.register<GenerateSourceTemplates>(
            getTaskName("template", "sources")
        ) {
            filteringCharset = Charsets.UTF_8.name()
            from(javaSrcDir)
            into(javaDestination)
            filter<ReplaceTokens>("tokens" to replacements())
        }
        java.srcDir(javaGenerateTask.map { it.outputs })

        val resourcesDestination = layout.buildDirectory.dir("generated/resources/templates/$name")
        val resourcesSrcDir = layout.projectDirectory.dir("src/$name/resourceTemplates")
        val resourcesGenerateTask = tasks.register<GenerateResourceTemplates>(
            getTaskName("template", "resources")
        ) {
            filteringCharset = Charsets.UTF_8.name()
            from(resourcesSrcDir)
            into(resourcesDestination)
            filter<ReplaceTokens>("tokens" to replacements())
        }
        resources.srcDir(resourcesGenerateTask.map { it.outputs })
    }

    return configureIdeSync(
        tasks.register("allTemplateSources") {
            dependsOn(tasks.withType<GenerateSourceTemplates>())
        },
        tasks.register("allTemplateResources") {
            dependsOn(tasks.withType<GenerateResourceTemplates>())
        }
    )
}

fun Project.configureIdeSync(vararg generateAllTasks: TaskProvider<Task>) {
    extensions.findByType<EclipseModel> {
        synchronizationTasks(generateAllTasks)
    }

    extensions.findByType<IdeaModel> {
        if (project != null) {
            (project as ExtensionAware).extensions.configure<ProjectSettings> {
                (this as ExtensionAware).extensions.configure<TaskTriggersConfig> {
                    afterSync(generateAllTasks)
                }
            }
        }
    }

    //todo wasn't able to find something for VS(Code)
}

inline fun <reified T : Any> ExtensionContainer.findByType(noinline action: T.() -> Unit) {
    val extension = findByType(T::class)
    if (extension != null) {
        action.invoke(extension)
    }
}

abstract class GenerateAnyTemplates @Inject constructor() : Copy() {
    private val replacements = mutableMapOf<String, String>()

    fun replaceToken(key: String, value: () -> Any) {
        replaceToken(key, value.invoke())
    }

    fun replaceToken(key: String, value: Any) {
        replacements[key] = value.toString()
    }

    fun replacements(): Map<String, String> = replacements
}

abstract class GenerateResourceTemplates @Inject constructor() : GenerateAnyTemplates()
abstract class GenerateSourceTemplates @Inject constructor() : GenerateAnyTemplates()
