package io.github.stardust824

import kotlin.test.Test
import kotlin.test.assertEquals

class PlantMatcherTest {

    private val plants = PlantParser.parse(
        """
        plants:
          - name: "Red huckleberry"
            scientific_name: "Vaccinium parvifolium"
            traits:
              leaf_size: 3
              flower_color: ["green", "yellow", "pink"]
          - name: "Evergreen huckleberry"
            scientific_name: "Vaccinium ovatum"
            traits:
              leaf_size: [2, 5]
              flower_color: "pink"
          - name: "Douglas fir"
            scientific_name: "Pseudotsuga menziesii"
            traits:
              needle_length: [2, 3]
          - name: "Bigleaf Maple"
            scientific_name: "Acer macrophyllum"
            traits:
              leaf_size: [15, 30]
              flower_color: ["green", "yellow"]
        """.trimIndent()
    )
    private val matcher = PlantMatcher(plants)

    private fun distanceOf(name: String, answers: Map<String, TraitValue>) =
        matcher.distance(plants.single { it.name == name }, answers)

    private fun labels(vararg v: String) = TraitValue.Categorical(v.toSet())
    private fun number(x: Double) = TraitValue.Numeric(x, x)

    @Test
    fun scaleCoversAllPlants() {
        assertEquals(2.0..30.0, matcher.scales["leaf_size"])
    }

    @Test
    fun labelMatchIsZeroEvenWhenPlantHasOtherLabels() {
        assertEquals(0.0, distanceOf("Red huckleberry", mapOf("flower_color" to labels("pink"))))
        assertEquals(1.0, distanceOf("Bigleaf Maple", mapOf("flower_color" to labels("pink"))))
    }

    @Test
    fun numberInsideRangeIsZeroOutsideIsScaledGap() {
        val answers = mapOf("leaf_size" to number(4.0))
        assertEquals(0.0, distanceOf("Evergreen huckleberry", answers))
        assertEquals(1.0 / 28.0, distanceOf("Red huckleberry", answers), 1e-9)
        assertEquals(11.0 / 28.0, distanceOf("Bigleaf Maple", answers), 1e-9)
    }

    @Test
    fun missingTraitCostsHalf() {
        assertEquals(0.5, distanceOf("Douglas fir", mapOf("flower_color" to labels("pink"))))
    }

    @Test
    fun unansweredTraitsAreIgnored() {
        assertEquals(0.0, distanceOf("Douglas fir", emptyMap()))
    }

    @Test
    fun scoreAndQualityForTheMatchBar() {
        val ranked = matcher.rank(mapOf("flower_color" to labels("pink"), "leaf_size" to number(4.0)))
            .associateBy { it.plant.name }

        // Perfect match → full green bar.
        assertEquals(1.0, ranked.getValue("Evergreen huckleberry").score)
        assertEquals(MatchQuality.GOOD, ranked.getValue("Evergreen huckleberry").quality)
        // Missing both traits (0.5 each): 1 - sqrt(0.5) / sqrt(2) = 0.5 → yellow.
        assertEquals(0.5, ranked.getValue("Douglas fir").score, 1e-9)
        assertEquals(MatchQuality.OKAY, ranked.getValue("Douglas fir").quality)
        // Wrong color and leaves too big → red.
        assertEquals(MatchQuality.POOR, ranked.getValue("Bigleaf Maple").quality)
    }

    @Test
    fun ranksClosestFirst() {
        val ranked = matcher.rank(mapOf("flower_color" to labels("pink"), "leaf_size" to number(4.0)))
        assertEquals(
            listOf("Evergreen huckleberry", "Red huckleberry", "Douglas fir", "Bigleaf Maple"),
            ranked.map { it.plant.name },
        )
    }
}
