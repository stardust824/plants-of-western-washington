package io.github.stardust824

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IdQuestionsTest {

    private val plants = PlantParser.parse(
        """
        plants:
          - name: "Red huckleberry"
            scientific_name: "Vaccinium parvifolium"
            traits:
              flower_color: ["green", "yellow", "pink"]
              leaf_size: 3
              plant_type: "shrub"
          - name: "Bigleaf Maple"
            scientific_name: "Acer macrophyllum"
            traits:
              flower_color: ["green", "white"]
              leaf_size: [15, 30]
              plant_type: "tree"
              # Made-up trait that isn't in IdQuestions' known list, to test the "Other" section.
              smell: "sweet"
        """.trimIndent()
    )

    private val questions = IdQuestions.build(plants)

    @Test
    fun questionsAreSortedBySection() {
        // General → Leaves → Flowers → Other, regardless of the order in the YAML.
        // "foliage" isn't in the YAML: it's worked out from leaf_size.
        assertEquals(listOf("plant_type", "foliage", "leaf_size", "flower_color", "smell"), questions.map { it.trait })
    }

    @Test
    fun foliageIsWorkedOutFromLeafAndNeedleTraits() {
        val parsed = PlantParser.parse(
            """
            plants:
              - name: "Leafy"
                scientific_name: "a"
                traits:
                  leaf_size: 3
              - name: "Needly"
                scientific_name: "b"
                traits:
                  needle_length: [2, 3]
              - name: "Both"
                scientific_name: "c"
                traits:
                  leaf_shape: "oval"
                  needle_length: 1
              - name: "Neither"
                scientific_name: "d"
                traits:
                  plant_type: "moss"
            """.trimIndent()
        ).associate { it.name to it.traits[FOLIAGE_TRAIT] }

        assertEquals(TraitValue.Categorical(setOf("leaves")), parsed["Leafy"])
        assertEquals(TraitValue.Categorical(setOf("needles")), parsed["Needly"])
        assertEquals(TraitValue.Categorical(setOf("leaves", "needles")), parsed["Both"])
        assertNull(parsed["Neither"])
    }

    @Test
    fun pickingNeedlesHidesLeafQuestionsAndViceVersa() {
        val all = IdQuestions.build(
            PlantParser.parse(
                """
                plants:
                  - name: "Maple"
                    scientific_name: "a"
                    traits:
                      leaf_size: [15, 30]
                      flower_color: "green"
                  - name: "Fir"
                    scientific_name: "b"
                    traits:
                      needle_length: [2, 3]
                      cone_size: [5, 10]
                """.trimIndent()
            )
        )
        fun visibleTraits(choices: Map<String, String>) = IdQuestions.visible(all, choices).map { it.trait }

        assertEquals(
            listOf("foliage", "leaf_size", "needle_length", "cone_size", "flower_color"),
            visibleTraits(emptyMap()),
        )
        assertEquals(
            listOf("foliage", "needle_length", "cone_size", "flower_color"),
            visibleTraits(mapOf("foliage" to "needles")),
        )
        assertEquals(
            listOf("foliage", "leaf_size", "flower_color"),
            visibleTraits(mapOf("foliage" to "leaves")),
        )
    }

    @Test
    fun choiceOptionsComeFromAllPlantsSortedWithoutDuplicates() {
        val flower = questions.single { it.trait == "flower_color" }
        assertEquals(QuestionKind.Choice(listOf("green", "pink", "white", "yellow")), flower.kind)
    }

    @Test
    fun numberTraitsGetAUnit() {
        assertEquals(QuestionKind.Number("cm"), questions.single { it.trait == "leaf_size" }.kind)
    }

    @Test
    fun unknownTraitsGoToOtherWithAGeneratedLabel() {
        val smell = questions.single { it.trait == "smell" }
        assertEquals(QuestionGroup.OTHER, smell.group)
        assertEquals("Smell", smell.label)
    }

    @Test
    fun answersSkipBlankOrInvalidNumbers() {
        val answers = IdQuestions.answers(
            choices = mapOf("flower_color" to "pink"),
            numbers = mapOf("leaf_size" to "4,5", "flower_size" to "abc", "cone_size" to " "),
        )
        assertEquals(
            mapOf(
                "flower_color" to TraitValue.Categorical(setOf("pink")),
                "leaf_size" to TraitValue.Numeric(4.5, 4.5),
            ),
            answers,
        )
    }

    @Test
    fun parseNumberAcceptsHalfTypedInputButNotText() {
        assertEquals(4.0, IdQuestions.parseNumber("4."))
        assertNull(IdQuestions.parseNumber("4cm"))
    }
}
