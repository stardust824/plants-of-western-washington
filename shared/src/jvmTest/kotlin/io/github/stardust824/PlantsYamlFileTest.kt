package io.github.stardust824

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the real bundled data files, so mistakes are caught by tests:
 * plants.yaml parses, and descriptions.yaml / photo_credits.yaml / the photos folder all line up with it.
 */
class PlantsYamlFileTest {

    private val dir = File("src/commonMain/composeResources/files")
    private val plants = PlantParser.parse(File(dir, "plants.yaml").readText())
    private val descriptions = PlantExtrasParser.parseDescriptions(File(dir, "descriptions.yaml").readText())
    private val photos = PlantExtrasParser.parsePhotoCredits(File(dir, "photo_credits.yaml").readText())
    private val plantIds = plants.map { it.id }.toSet()

    @Test
    fun bundledYamlParses() {
        assertTrue(plants.isNotEmpty())
        assertEquals(plants.size, plantIds.size, "Duplicate plant names")
    }

    @Test
    fun descriptionsAndCreditsOnlyNameRealPlants() {
        // Catches typos like "big_leaf_maple" that would silently never show up.
        assertEquals(emptySet(), descriptions.keys - plantIds, "descriptions.yaml names plants not in plants.yaml")
        assertEquals(emptySet(), photos.keys - plantIds, "photo_credits.yaml names plants not in plants.yaml")
    }

    @Test
    fun everyListedPhotoExists() {
        val missing = photos.values.flatten().map { it.file }.filterNot { File(dir, "photos/$it").exists() }
        assertEquals(emptyList(), missing, "Listed in photo_credits.yaml but not in the photos folder")
    }

    @Test
    fun everyPhotoInTheFolderHasACredit() {
        val listed = photos.values.flatten().map { it.file }.toSet()
        val unlisted = File(dir, "photos").list().orEmpty().filterNot { it.startsWith(".") || it in listed }
        assertEquals(emptyList(), unlisted, "In the photos folder but not in photo_credits.yaml")
    }

    @Test
    fun everyPlantHasADescriptionAndAPhoto() {
        assertEquals(emptyList(), plants.map { it.id }.filterNot { it in descriptions }, "Plants without a description")
        assertEquals(emptyList(), plants.map { it.id }.filter { photos[it].isNullOrEmpty() }, "Plants without a photo")
    }
}
