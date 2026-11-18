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
        val from = source.get().asFile
        val to = output.get().asFile
        to.deleteRecursively()
        val blockstates = mutableMapOf<String, String>()
        val models = mutableMapOf<String, String>()
        val files = from.walkTopDown().filter { it.isFile }
            .map { it to it.relativeTo(from).invariantSeparatorsPath }
            .filterNot { it.second.startsWith(".cache/") }
            .toList()
        for ((file, relative) in files) {
            val parts = relative.split('/')
            if (parts.size > 3 && parts[0] == "assets" && file.extension == "json") {
                if (parts[2] == "blockstates") blockstates[relative] = file.readText()
                if (parts[2] == "models") models[relative] = file.readText()
            }
        }
        val conversion = ResourceConversion(minecraftVersion.get(), loaderName.get(), models)
        for ((file, relative) in files) {
            val target = to.resolve(conversion.path(relative))
            target.parentFile.mkdirs()
            if (file.extension == "json") {
                target.writeText(conversion.json(relative, file.readText()))
            } else {
                file.copyTo(target, overwrite = true)
            }
            val parts = relative.split('/')
            if (conversion.testStructuresAsSnbt && parts.size == 4 && parts[0] == "data" && parts[2] == "structure" && file.extension == "nbt") {
                val snbt = to.resolve("data/${parts[1]}/gametest/structures/${file.nameWithoutExtension}.snbt")
                snbt.parentFile.mkdirs()
                snbt.writeText(Snbt.fromNbt(file))
            }
        }
        if (conversion.itemLayersInCode) {
            conversion.itemLayers().entries
                .groupBy { it.key.substringBefore(':') }
                .forEach { (namespace, entries) ->
                    val target = to.resolve("assets/$namespace/item_layers.json")
                    target.writeText(conversion.layerTable(entries.associate { it.key to it.value }))
                }
        }
        if (conversion.renderTypesInCode) {
            conversion.renderLayers(blockstates, models).entries
                .groupBy { it.key.substringBefore(':') }
                .forEach { (namespace, entries) ->
                    val target = to.resolve("assets/$namespace/render_layers.json")
                    target.writeText(conversion.layerTable(entries.associate { it.key to it.value }))
                }
        }
    }
}
