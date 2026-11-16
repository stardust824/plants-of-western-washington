package io.github.stardust824

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlantExtrasParserTest {

    @Test
    fun plantIdIsLowercaseWithUnderscores() {
        assertEquals("bigleaf_maple", plantId("Bigleaf Maple"))
        assertEquals("red_huckleberry", plantId("  Red huckleberry "))
    }

    @Test
    fun descriptionsAreTrimmedAndBlankOnesSkipped() {
        val descriptions = PlantExtrasParser.parseDescriptions(
            """
            red_huckleberry: >
              A shrub with
              red berries.
            douglas_fir: ""
            """.trimIndent()
        )
        assertEquals(mapOf("red_huckleberry" to "A shrub with red berries."), descriptions)
    }

    @Test
    fun photoCreditsKeepOrderAndSkipEmptyFiles() {
        val photos = PlantExtrasParser.parsePhotoCredits(
            """
            evergreen_huckleberry:
              - file: "evergreen_huckleberry_1.jpg"
                photographer: "Ben Sims"
                license: "Public Domain"
                source: "iNaturalist"
              - file: "evergreen_huckleberry_2.jpg"
                photographer: "E Redick"
                license: "CC BY-NC-ND"
                source: "iNaturalist"
            douglas_fir:
              - file: ""
                photographer: ""
                license: ""
                source: ""
            """.trimIndent()
        )
        assertEquals(
            listOf("evergreen_huckleberry_1.jpg", "evergreen_huckleberry_2.jpg"),
            photos.getValue("evergreen_huckleberry").map { it.file },
        )
        assertEquals(emptyList(), photos.getValue("douglas_fir"))
    }

    @Test
    fun creditLeavesOutBlankFields() {
        assertEquals("Photo: Sonia S · CC BY-NC · iNaturalist", PlantPhoto("a.jpg", "Sonia S", "CC BY-NC", "iNaturalist").credit)
        assertEquals("Photo: Me", PlantPhoto("a.jpg", photographer = "Me").credit)
        assertEquals("", PlantPhoto("a.jpg").credit)
    }

    @Test
    fun noDerivativesIsDetectedFromTheLicense() {
        assertTrue(PlantPhoto("a.jpg", license = "CC BY-NC-ND").noDerivatives)
        assertTrue(PlantPhoto("a.jpg", license = "cc by-nd 4.0").noDerivatives)
        assertFalse(PlantPhoto("a.jpg", license = "CC BY-NC").noDerivatives)
        assertFalse(PlantPhoto("a.jpg", license = "Public Domain").noDerivatives)
        assertFalse(PlantPhoto("a.jpg", license = "").noDerivatives)
    }

    @Test
    fun attachMatchesByPlantId() {
        val plants = PlantParser.parse(
            """
            plants:
              - name: "Red huckleberry"
                scientific_name: "Vaccinium parvifolium"
                traits:
                  plant_type: "shrub"
            """.trimIndent()
        )
        val attached = PlantExtrasParser.attach(
            plants,
            descriptions = mapOf("red_huckleberry" to "Red berries."),
            photos = mapOf("red_huckleberry" to listOf(PlantPhoto("red_huckleberry_1.jpg"))),
        ).single()
        assertEquals("Red berries.", attached.description)
        assertEquals("red_huckleberry_1.jpg", attached.photos.single().file)
    }
}
