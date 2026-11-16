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
        val converted = if (forgeNames) forgeIds(root) else root
        return GSON.toJson(converted)
    }

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
