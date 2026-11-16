package io.github.stardust824

/** Sections of the ID screen, in the order they appear. */
enum class QuestionGroup(val title: String) {
    GENERAL("General"),
    LEAVES("Leaves"),
    NEEDLES_AND_CONES("Needles and cones"),
    FLOWERS("Flowers"),
    BERRIES("Berries"),
    BARK_AND_STEM("Bark and stem"),
    // Any trait not listed in KNOWN_TRAITS below ends up here, so new traits still show up.
    OTHER("Other"),
}

/** What kind of input a question needs. */
sealed interface QuestionKind {
    /** Pick one of these labels (shown as chips). */
    data class Choice(val options: List<String>) : QuestionKind

    /** Type a number (shown as a text box), optionally with a unit like "cm". */
    data class Number(val unit: String?) : QuestionKind
}

/** One question on the ID screen, e.g. "Flower color" with options green / pink / ... */
data class Question(
    val trait: String,
    val label: String,
    val group: QuestionGroup,
    val kind: QuestionKind,
)

/** Hand-written display info for traits we know about. */
private data class TraitInfo(
    val group: QuestionGroup,
    val label: String? = null, // null = make a label from the trait name
    val unit: String? = null,
)

/**
 * Known traits, in the order they appear within their group.
 * To add a new trait to plants.yaml, it works without touching this list (it goes under "Other"),
 * but adding it here puts it in the right section with a nicer label and unit.
 */
private val KNOWN_TRAITS: List<Pair<String, TraitInfo>> = listOf(
    "plant_type" to TraitInfo(QuestionGroup.GENERAL),
    FOLIAGE_TRAIT to TraitInfo(QuestionGroup.GENERAL, label = "Leaves or needles?"),
    "elevation" to TraitInfo(QuestionGroup.GENERAL),

    "leaf_shape" to TraitInfo(QuestionGroup.LEAVES),
    "leaf_edge" to TraitInfo(QuestionGroup.LEAVES),
    "leaf_arrangement" to TraitInfo(QuestionGroup.LEAVES),
    "leaf_size" to TraitInfo(QuestionGroup.LEAVES, unit = "cm"),

    "needle_length" to TraitInfo(QuestionGroup.NEEDLES_AND_CONES, unit = "cm"),
    "cone_size" to TraitInfo(QuestionGroup.NEEDLES_AND_CONES, unit = "cm"),

    "flower_color" to TraitInfo(QuestionGroup.FLOWERS),
    "num_petals" to TraitInfo(QuestionGroup.FLOWERS, label = "Number of petals", unit = "petals"),
    "flower_size" to TraitInfo(QuestionGroup.FLOWERS, unit = "cm"),

    "berry_color" to TraitInfo(QuestionGroup.BERRIES),

    "bark_stem_color" to TraitInfo(QuestionGroup.BARK_AND_STEM, label = "Bark or stem color"),
    "bark_texture" to TraitInfo(QuestionGroup.BARK_AND_STEM),
)

/**
 * Sections to hide depending on an answer: picking "needles" hides the leaf questions,
 * and picking "leaves" hides the needle and cone questions.
 */
private val HIDDEN_GROUPS: Map<Pair<String, String>, Set<QuestionGroup>> = mapOf(
    (FOLIAGE_TRAIT to FOLIAGE_NEEDLES) to setOf(QuestionGroup.LEAVES),
    (FOLIAGE_TRAIT to FOLIAGE_LEAVES) to setOf(QuestionGroup.NEEDLES_AND_CONES),
)

object IdQuestions {

    /** The questions to show, given the chips selected so far (see HIDDEN_GROUPS). */
    fun visible(questions: List<Question>, choices: Map<String, String>): List<Question> {
        val hidden = choices.flatMap { (trait, option) -> HIDDEN_GROUPS[trait to option].orEmpty() }.toSet()
        return questions.filter { it.group !in hidden }
    }

    /**
     * Builds the list of questions from the plant data, sorted by section.
     * Choice options come from the data, so a new label in plants.yaml becomes a new chip.
     */
    fun build(plants: List<Plant>): List<Question> {
        // Gather every value each trait takes across all plants.
        val valuesByTrait = mutableMapOf<String, MutableList<TraitValue>>()
        for (plant in plants) {
            for ((trait, value) in plant.traits) {
                valuesByTrait.getOrPut(trait) { mutableListOf() }.add(value)
            }
        }

        val knownOrder = KNOWN_TRAITS.map { it.first }
        val knownInfo = KNOWN_TRAITS.toMap()

        return valuesByTrait.map { (trait, values) ->
            val info = knownInfo[trait]
            val kind = if (values.all { it is TraitValue.Numeric }) {
                // Every plant stores a number for this trait → number box.
                QuestionKind.Number(info?.unit)
            } else {
                // Otherwise offer every label any plant uses, alphabetically.
                val options = values
                    .filterIsInstance<TraitValue.Categorical>()
                    .flatMap { it.values }
                    .distinct()
                    .sorted()
                QuestionKind.Choice(options)
            }
            Question(
                trait = trait,
                label = info?.label ?: defaultLabel(trait),
                group = info?.group ?: QuestionGroup.OTHER,
                kind = kind,
            )
        }.sortedWith(
            compareBy(
                { it.group.ordinal }, // section order
                { knownOrder.indexOf(it.trait).let { i -> if (i == -1) Int.MAX_VALUE else i } }, // known order
                { it.trait }, // unknown traits alphabetically
            )
        )
    }

    /**
     * Turns what the user has entered into answers for [PlantMatcher].
     *
     * [choices] maps trait → the selected chip; [numbers] maps trait → the raw text typed in a box.
     * Blank or invalid numbers are left out, so they count as "not answered".
     */
    fun answers(choices: Map<String, String>, numbers: Map<String, String>): Map<String, TraitValue> {
        val result = mutableMapOf<String, TraitValue>()
        for ((trait, option) in choices) {
            result[trait] = TraitValue.Categorical(setOf(option))
        }
        for ((trait, text) in numbers) {
            val number = parseNumber(text) ?: continue
            result[trait] = TraitValue.Numeric(number, number)
        }
        return result
    }

    /** "4", "4.5" and "4,5" all work; anything else is null. */
    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    /** "leaf_edge" → "Leaf edge" */
    private fun defaultLabel(trait: String): String =
        trait.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
