import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ConvertResources : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: DirectoryProperty

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Input
    abstract val loaderName: Property<String>

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun convert() {
        val conversion = ResourceConversion(minecraftVersion.get(), loaderName.get())
        val from = source.get().asFile
        val to = output.get().asFile
        to.deleteRecursively()
        from.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(from).invariantSeparatorsPath
            if (relative.startsWith(".cache/")) return@forEach
            val target = to.resolve(conversion.path(relative))
            target.parentFile.mkdirs()
            if (file.extension == "json") {
                target.writeText(conversion.json(relative, file.readText()))
            } else {
                file.copyTo(target, overwrite = true)
            }
        }
    }
}
