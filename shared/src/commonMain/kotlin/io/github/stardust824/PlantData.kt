package io.github.stardust824

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import plant_app.shared.generated.resources.Res

/**
 * A single trait value from plants.yaml.
 *
 * The YAML allows a trait to be a single value or a list, so we normalize into two shapes:
 * - [Categorical]: one or more labels, e.g. `flower_color: ["green", "yellow"]`
 * - [Numeric]: a min/max range, e.g. `leaf_size: [2, 5]`; a single number `3` becomes 3..3
 */
sealed interface TraitValue {
    data class Categorical(val values: Set<String>) : TraitValue
    data class Numeric(val min: Double, val max: Double) : TraitValue {
        init {
            require(min <= max) { "Numeric range min ($min) must be <= max ($max)" }
        }
    }
}

data class Plant(
    val name: String,
    val scientificName: String,
    val traits: Map<String, TraitValue>,
    /** From descriptions.yaml; empty until written. */
    val description: String = "",
    /** From photo_credits.yaml, in order; the first is the main photo. */
    val photos: List<PlantPhoto> = emptyList(),
) {
    /**
     * The plant's name in lowercase with underscores, e.g. "Bigleaf Maple" → "bigleaf_maple".
     * Used to look it up in descriptions.yaml and photo_credits.yaml, and in screen links.
     */
    val id: String = plantId(name)
}

/** One photo of a plant and who to credit for it. Blank fields are left out when shown. */
data class PlantPhoto(
    /** File name inside composeResources/files/photos, e.g. "red_huckleberry_1.jpg". */
    val file: String,
    val photographer: String = "",
    val license: String = "",
    val source: String = "",
) {
    /**
     * True for "no derivatives" licenses (e.g. "CC BY-NC-ND"): the photo must not be altered,
     * so the app always shows it whole and never crops it.
     */
    val noDerivatives: Boolean
        get() = Regex("""\bND\b""").containsMatchIn(license.uppercase())

    /** e.g. "Photo: Sonia S · CC BY-NC · iNaturalist" */
    val credit: String
        get() = listOf(photographer, license, source).filter { it.isNotBlank() }
            .joinToString(" · ")
            .let { if (it.isEmpty()) "" else "Photo: $it" }
}

/** "Bigleaf Maple" → "bigleaf_maple" */
fun plantId(name: String): String =
    name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

class PlantParseException(message: String) : Exception(message)

/** The "leaves or needles" trait, worked out from each plant's other traits (see PlantParser). */
const val FOLIAGE_TRAIT = "foliage"
const val FOLIAGE_LEAVES = "leaves"
const val FOLIAGE_NEEDLES = "needles"

object PlantParser {

    fun parse(yamlText: String): List<Plant> {
        val root = Yaml.default.parseToYamlNode(yamlText) as? YamlMap
            ?: throw PlantParseException("Top level of the YAML must be a map")
        val plants = root.get<YamlList>("plants")
            ?: throw PlantParseException("Missing top-level 'plants' list")

        return plants.items.mapIndexed { index, node ->
            val plantMap = node as? YamlMap
                ?: throw PlantParseException("Plant #$index is not a map")
            parsePlant(plantMap, index)
        }
    }

    private fun parsePlant(map: YamlMap, index: Int): Plant {
        val name = map.requireString("name", "plant #$index")
        val scientificName = map.requireString("scientific_name", name)
        val traitsMap = map.get<YamlMap>("traits")
            ?: throw PlantParseException("Plant '$name' has no 'traits' map")

        val traits = traitsMap.entries.entries.associate { (key, value) ->
            val traitName = key.content
            traitName to parseTraitValue(value, "$name.$traitName")
        }
        return Plant(name, scientificName, withFoliage(traits))
    }

    /**
     * Adds a "foliage" trait worked out from the plant's other traits, so plants.yaml doesn't need one:
     * any needle_... trait (e.g. needle_length) → "needles"; any leaf_... trait → "leaves"; both → both.
     * Plants with neither get no foliage trait. A foliage trait written in plants.yaml is kept as-is.
     */
    private fun withFoliage(traits: Map<String, TraitValue>): Map<String, TraitValue> {
        if (FOLIAGE_TRAIT in traits) return traits
        val foliage = buildSet {
            if (traits.keys.any { it.startsWith("needle_") }) add(FOLIAGE_NEEDLES)
            if (traits.keys.any { it.startsWith("leaf_") }) add(FOLIAGE_LEAVES)
        }
        return if (foliage.isEmpty()) traits else traits + (FOLIAGE_TRAIT to TraitValue.Categorical(foliage))
    }

    private fun parseTraitValue(node: YamlNode, where: String): TraitValue {
        val scalars: List<YamlScalar> = when (node) {
            is YamlScalar -> listOf(node)
            is YamlList -> node.items.map {
                it as? YamlScalar ?: throw PlantParseException("$where: nested lists/maps are not supported")
            }
            is YamlNull -> throw PlantParseException("$where: value is empty")
            else -> throw PlantParseException("$where: unsupported value type")
        }
        if (scalars.isEmpty()) throw PlantParseException("$where: list is empty")

        // Numeric only if every entry is a number; otherwise treat everything as labels.
        val numbers = scalars.map { it.content.toDoubleOrNull() }
        if (numbers.all { it != null }) {
            val nums = numbers.filterNotNull()
            if (nums.size > 2) {
                throw PlantParseException("$where: numeric ranges must be a single value or [min, max]")
            }
            return TraitValue.Numeric(nums.min(), nums.max())
        }
        return TraitValue.Categorical(scalars.map { it.content.trim().lowercase() }.toSet())
    }

    private fun YamlMap.requireString(key: String, where: String): String =
        get<YamlScalar>(key)?.content
            ?: throw PlantParseException("$where: missing '$key'")
}

/** Reads descriptions.yaml and photo_credits.yaml and attaches them to the plants. */
object PlantExtrasParser {

    /** descriptions.yaml: plant id → description text. Blank ("") entries are left out. */
    fun parseDescriptions(yamlText: String): Map<String, String> {
        val root = Yaml.default.parseToYamlNode(yamlText) as? YamlMap
            ?: throw PlantParseException("descriptions.yaml: top level must be a map of plant names")
        return root.entries.entries.mapNotNull { (key, value) ->
            // ">" text comes back with a trailing newline, so trim it.
            val text = (value as? YamlScalar)?.content?.trim().orEmpty()
            if (text.isEmpty()) null else key.content to text
        }.toMap()
    }

    /** photo_credits.yaml: plant id → its photos, in order. Entries with no file are skipped. */
    fun parsePhotoCredits(yamlText: String): Map<String, List<PlantPhoto>> {
        val root = Yaml.default.parseToYamlNode(yamlText) as? YamlMap
            ?: throw PlantParseException("photo_credits.yaml: top level must be a map of plant names")
        return root.entries.entries.associate { (key, value) ->
            val id = key.content
            val list = value as? YamlList
                ?: throw PlantParseException("photo_credits.yaml: '$id' must be a list of '- file:' entries")
            id to list.items.mapNotNull { item ->
                val map = item as? YamlMap
                    ?: throw PlantParseException("photo_credits.yaml: each photo under '$id' must start with '- file:'")
                fun field(name: String) = map.get<YamlScalar>(name)?.content?.trim().orEmpty()
                val file = field("file")
                if (file.isEmpty()) null
                else PlantPhoto(file, field("photographer"), field("license"), field("source"))
            }
        }
    }

    /** Returns copies of [plants] with their descriptions and photos filled in, matched by [Plant.id]. */
    fun attach(
        plants: List<Plant>,
        descriptions: Map<String, String>,
        photos: Map<String, List<PlantPhoto>>,
    ): List<Plant> = plants.map { plant ->
        plant.copy(
            description = descriptions[plant.id].orEmpty(),
            photos = photos[plant.id].orEmpty(),
        )
    }
}

/** Loads plants.yaml, descriptions.yaml and photo_credits.yaml from compose resources. */
suspend fun loadPlants(): List<Plant> {
    suspend fun read(name: String) = Res.readBytes("files/$name").decodeToString()
    return PlantExtrasParser.attach(
        plants = PlantParser.parse(read("plants.yaml")),
        descriptions = PlantExtrasParser.parseDescriptions(read("descriptions.yaml")),
        photos = PlantExtrasParser.parsePhotoCredits(read("photo_credits.yaml")),
    )
}

/**
 * Loads the plants once and shares them between screens (ID, Browse, plant pages),
 * so the YAML files aren't re-read every time a screen opens.
 */
object PlantRepository {
    private val mutex = Mutex()
    private var cached: List<Plant>? = null

    suspend fun plants(): List<Plant> = mutex.withLock {
        cached ?: loadPlants().also { cached = it }
    }

    suspend fun plant(id: String): Plant? = plants().firstOrNull { it.id == id }
}
