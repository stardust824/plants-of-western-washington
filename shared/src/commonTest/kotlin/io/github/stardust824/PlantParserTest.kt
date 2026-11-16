package io.github.stardust824

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlantParserTest {

    private val sample = """
        plants:
          - name: "Red huckleberry"
            scientific_name: "Vaccinium parvifolium"
            traits:
              plant_type: "shrub"
              # in cm
              leaf_size: 3
              leaf_size_range: [2, 5]
              flower_size: .5
              flower_color: ["Green", "yellow", "pink"]
    """.trimIndent()

    @Test
    fun parsesNamesAndTraits() {
        val plants = PlantParser.parse(sample)
        assertEquals(1, plants.size)
        val p = plants.single()
        assertEquals("Red huckleberry", p.name)
        assertEquals("Vaccinium parvifolium", p.scientificName)
        assertEquals(TraitValue.Categorical(setOf("shrub")), p.traits["plant_type"])
        assertEquals(TraitValue.Numeric(3.0, 3.0), p.traits["leaf_size"])
        assertEquals(TraitValue.Numeric(2.0, 5.0), p.traits["leaf_size_range"])
        assertEquals(TraitValue.Numeric(0.5, 0.5), p.traits["flower_size"])
        assertEquals(
            TraitValue.Categorical(setOf("green", "yellow", "pink")),
            p.traits["flower_color"],
        )
    }

    @Test
    fun rejectsMissingName() {
        assertFailsWith<PlantParseException> {
            PlantParser.parse(
                """
                plants:
                  - scientific_name: "x"
                    traits:
                      plant_type: "tree"
                """.trimIndent()
            )
        }
    }
}
