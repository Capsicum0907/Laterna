import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

class ResourceConversion(private val minecraft: String, private val loader: String) {
    private val pluralFolders = !Targets.atLeast(minecraft, "1.21")
    private val recipeResultIsItem = !Targets.atLeast(minecraft, "1.20.5")
    private val itemPredicateIsList = !Targets.atLeast(minecraft, "1.20.5")
    private val forgeNames = loader == "forge"
    val renderTypesInCode = !(loader == "neoforge" || (loader == "forge" && Targets.atLeast(minecraft, "1.19")))

    fun path(path: String): String {
        if (!pluralFolders) return path
        val parts = path.split('/').toMutableList()
        if (parts.size < 3 || parts[0] != "data") return path
        parts[2] = SINGULAR_TO_PLURAL[parts[2]] ?: parts[2]
        if (parts[2] == "tags" && parts.size > 3) parts[3] = SINGULAR_TO_PLURAL[parts[3]] ?: parts[3]
        return parts.joinToString("/")
    }

    fun json(path: String, text: String): String {
        val root = JsonParser.parseString(text)
        val parts = path.split('/')
        val folder = if (parts.size > 2) parts[2] else ""
        if (parts[0] == "data" && folder == "recipe" && recipeResultIsItem) recipeResult(root)
        if (parts[0] == "data" && folder == "advancement" && itemPredicateIsList) itemPredicates(root)
        if (parts[0] == "assets" && folder == "models" && renderTypesInCode) modelLayers(root.asJsonObject)
        val converted = if (forgeNames) forgeIds(root) else root
        return GSON.toJson(converted)
    }

    private fun modelLayers(model: JsonObject) {
        if (model.get("loader")?.asString != COMPOSITE) {
            model.remove("render_type")
            return
        }
        val layers = JsonObject()
        for ((_, child) in model.remove("children").asJsonObject.entrySet()) {
            val part = child.asJsonObject
            val layer = layerName(part.remove("render_type")?.asString)
            check(!layers.has(layer)) { "Two parts of one composite model share the $layer layer" }
            layers.add(layer, part)
        }
        model.remove("item_render_order")
        model.addProperty("loader", "forge:multi-layer")
        model.add("layers", layers)
    }

    fun renderLayers(blockstates: Map<String, String>, models: Map<String, String>): Map<String, List<String>> {
        val table = sortedMapOf<String, List<String>>()
        for ((path, text) in blockstates) {
            val parts = path.split('/')
            val block = parts[1] + ":" + parts.last().removeSuffix(".json")
            val used = mutableSetOf<String>()
            for (model in modelsOf(JsonParser.parseString(text).asJsonObject)) used += layersOf(model, models)
            val ordered = LAYERS.filter { it in used }
            if (ordered != listOf("solid")) table[block] = ordered
        }
        return table
    }

    private fun modelsOf(blockstate: JsonObject): List<String> {
        val found = mutableListOf<String>()
        fun take(element: JsonElement) {
            if (element is JsonArray) element.forEach(::take)
            else element.asJsonObject.get("model")?.let { found += it.asString }
        }
        blockstate.getAsJsonObject("variants")?.entrySet()?.forEach { take(it.value) }
        blockstate.getAsJsonArray("multipart")?.forEach { take(it.asJsonObject.get("apply")) }
        return found
    }

    private fun layersOf(id: String, models: Map<String, String>): List<String> {
        val namespace = id.substringBefore(':')
        val text = models["assets/$namespace/models/${id.substringAfter(':')}.json"] ?: return listOf("solid")
        val model = JsonParser.parseString(text).asJsonObject
        if (model.get("loader")?.asString == COMPOSITE) {
            return model.getAsJsonObject("children").entrySet()
                .map { layerName(it.value.asJsonObject.get("render_type")?.asString) }
        }
        model.get("render_type")?.let { return listOf(layerName(it.asString)) }
        val parent = model.get("parent")?.asString ?: return listOf("solid")
        return layersOf(if (':' in parent) parent else "minecraft:$parent", models)
    }

    private fun layerName(renderType: String?): String {
        val name = renderType?.substringAfter(':') ?: "solid"
        check(name in LAYERS) { "Unknown render type $renderType" }
        return name
    }

    fun layerTable(table: Map<String, List<String>>): String = GSON.toJson(table)

    private fun recipeResult(root: JsonElement) {
        val result = root.asJsonObject.get("result") as? JsonObject ?: return
        val id = result.remove("id") ?: return
        result.add("item", id)
    }

    private fun itemPredicates(root: JsonElement) {
        val criteria = root.asJsonObject.getAsJsonObject("criteria") ?: return
        for ((_, criterion) in criteria.entrySet()) {
            val items = criterion.asJsonObject.getAsJsonObject("conditions")?.get("items") as? JsonArray ?: continue
            for (predicate in items) {
                val obj = predicate as? JsonObject ?: continue
                val value = obj.get("items") as? JsonPrimitive ?: continue
                val id = value.asString
                if (id.startsWith("#")) {
                    obj.remove("items")
                    obj.addProperty("tag", id.substring(1))
                } else {
                    obj.add("items", JsonArray().apply { add(id) })
                }
            }
        }
    }

    private fun forgeIds(element: JsonElement): JsonElement = when {
        element is JsonObject -> JsonObject().also { out ->
            for ((key, value) in element.entrySet()) out.add(FORGE_KEYS[key] ?: key, forgeIds(value))
        }
        element is JsonArray -> JsonArray().also { out -> element.forEach { out.add(forgeIds(it)) } }
        element is JsonPrimitive && element.isString -> JsonPrimitive(FORGE_IDS[element.asString] ?: element.asString)
        else -> element
    }

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        private const val COMPOSITE = "neoforge:composite"

        private val LAYERS = listOf("solid", "cutout_mipped", "cutout", "translucent")

        private val SINGULAR_TO_PLURAL = mapOf(
            "recipe" to "recipes",
            "loot_table" to "loot_tables",
            "advancement" to "advancements",
            "structure" to "structures",
            "block" to "blocks",
            "item" to "items",
        )

        private val FORGE_KEYS = mapOf(
            "neoforge_data" to "forge_data",
        )

        private val FORGE_IDS = mapOf(
            "neoforge:composite" to "forge:composite",
            "c:dusts/redstone" to "forge:dusts/redstone",
            "c:glass_blocks" to "forge:glass",
            "c:glass_panes" to "forge:glass_panes",
            "c:nuggets/iron" to "forge:nuggets/iron",
            "c:stones" to "forge:stone",
        )
    }
}
