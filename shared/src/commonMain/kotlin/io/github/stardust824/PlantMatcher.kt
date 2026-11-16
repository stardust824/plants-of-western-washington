package io.github.stardust824

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * One result from [PlantMatcher.rank]: a plant and how far it is from the user's answers.
 * A distance of 0.0 means every answer matched. Bigger numbers mean a worse match.
 *
 * [score] is the same thing turned around and put on a 0.0–1.0 scale for showing to users:
 * 1.0 = every answer matched, 0.0 = every answer was a full mismatch.
 */
data class PlantMatch(val plant: Plant, val distance: Double, val score: Double) {
    val quality: MatchQuality
        get() = when {
            score >= MatchQuality.GOOD.minScore -> MatchQuality.GOOD
            score >= MatchQuality.OKAY.minScore -> MatchQuality.OKAY
            else -> MatchQuality.POOR
        }
}

/** Rough buckets for the match bar's color. */
enum class MatchQuality(val minScore: Double) {
    GOOD(0.75),
    OKAY(0.4),
    POOR(0.0),
}

/**
 * Ranks plants by how closely they match the traits the user describes.
 */
class PlantMatcher(private val plants: List<Plant>) {

    /**
     * For each number trait, the smallest and largest value found across ALL plants.
     * Example with the current plants.yaml: leaf_size → 2.0..30.0
     * (Evergreen huckleberry's leaves start at 2 cm, Bigleaf Maple's go up to 30 cm).
     *
     * This is calculated once, when the matcher is created, and used to scale number gaps.
     */
    val scales: Map<String, ClosedFloatingPointRange<Double>> = buildMap {
        // Look at every trait of every plant.
        for (plant in plants) {
            for ((trait, value) in plant.traits) {
                // Only number traits need a scale; skip label traits.
                if (value !is TraitValue.Numeric) continue

                // The scale we've built so far for this trait, or null if this is the first one we've seen.
                val current = get(trait)

                put(
                    trait,
                    if (current == null) {
                        // First plant with this trait: its own range is the scale so far.
                        value.min..value.max
                    } else {
                        // Widen the scale if this plant goes lower or higher than what we've seen.
                        min(current.start, value.min)..max(current.endInclusive, value.max)
                    },
                )
            }
        }
    }

    /**
     * Scores every plant against the user's answers and returns them closest match first.
     *
     * [answers] maps trait name → what the user said, e.g.
     *   "flower_color" → Categorical({"pink"}), "leaf_size" → Numeric(4.0, 4.0)
     */
    fun rank(answers: Map<String, TraitValue>): List<PlantMatch> =
        plants
            .map { plant ->
                val d = distance(plant, answers)
                PlantMatch(plant, d, score(d, answers.size))
            }
            .sortedBy { it.distance } // smallest distance (best match) first

    /**
     * Turns a distance into a 0.0–1.0 score.
     * Each answer's difference is at most 1.0, so the worst possible distance is sqrt(answer count).
     * Example: 2 answers, distance 0.5 → 1 - 0.5 / sqrt(2) = 0.65.
     */
    fun score(distance: Double, answerCount: Int): Double =
        if (answerCount == 0) 1.0 else (1.0 - distance / sqrt(answerCount.toDouble())).coerceIn(0.0, 1.0)

    /** Euclidean distance between one plant and the user's answers. 0.0 = perfect match. */
    fun distance(plant: Plant, answers: Map<String, TraitValue>): Double {
        var sum = 0.0
        // Only the traits the user answered are compared; everything else is ignored.
        for ((trait, answer) in answers) {
            // plant.traits[trait] is null when this plant doesn't have the trait.
            val d = traitDifference(trait, plant.traits[trait], answer)
            sum += d * d // square each difference...
        }
        return sqrt(sum) // ...then take the square root of the total.
    }

    /** The difference (0.0 to 1.0) for a single trait. */
    private fun traitDifference(trait: String, plantValue: TraitValue?, answer: TraitValue): Double =
        // `when` runs the first branch whose condition is true.
        // After an `is` check, Kotlin lets us use that type's fields directly (a "smart cast").
        when {
            // The plant doesn't have this trait at all.
            plantValue == null -> MISSING_TRAIT_PENALTY

            // Both are labels: a match if they share at least one label.
            plantValue is TraitValue.Categorical && answer is TraitValue.Categorical ->
                if (answer.values.any { it in plantValue.values }) 0.0 else 1.0

            // Both are numbers.
            plantValue is TraitValue.Numeric && answer is TraitValue.Numeric -> {
                // How far apart the user's value and the plant's range are; 0 if they overlap.
                // e.g. user says 4 cm, plant is 15..30 → gap = 15 - 4 = 11.
                //      user says 4 cm, plant is 2..5   → 4 is inside, gap = 0.
                // (Comparing against answer.min and answer.max also handles the user giving a range.)
                val gap = max(0.0, max(answer.min - plantValue.max, plantValue.min - answer.max))

                // How spread out this trait is across all plants, e.g. leaf_size: 30 - 2 = 28.
                val spread = scales[trait]?.let { it.endInclusive - it.start } ?: 0.0

                when {
                    // Inside the plant's range: perfect match.
                    gap == 0.0 -> 0.0
                    // Every plant has the same value, so we can't scale; count any gap as a full mismatch.
                    spread == 0.0 -> 1.0
                    // Scale the gap to 0..1, e.g. 11 / 28 = 0.39. Capped at 1.0 in case the user's
                    // value is further out than any plant in the data.
                    else -> min(1.0, gap / spread)
                }
            }

            // One is a label and the other is a number (shouldn't happen with valid input):
            // count it as a full mismatch.
            else -> 1.0
        }

    companion object {
        /**
         * Difference counted when a plant doesn't have a trait the user answered.
         * Halfway between a match (0.0) and a mismatch (1.0).
         */
        const val MISSING_TRAIT_PENALTY = 0.5
    }
}
