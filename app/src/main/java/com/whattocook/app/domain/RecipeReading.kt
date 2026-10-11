package com.whattocook.app.domain

/** Helpers that make a recipe quicker to read. */
object RecipeReading {

    enum class Difficulty { EASY, MEDIUM, HARD }

    /** A rough difficulty from how long it takes and how much there is to do. */
    fun difficulty(stepCount: Int, ingredientCount: Int, timeMinutes: Int): Difficulty {
        var score = 0
        score += when {
            timeMinutes > 60 -> 2
            timeMinutes > 30 -> 1
            else -> 0
        }
        score += when {
            stepCount > 10 -> 2
            stepCount > 6 -> 1
            else -> 0
        }
        score += when {
            ingredientCount > 14 -> 2
            ingredientCount > 9 -> 1
            else -> 0
        }
        return when {
            score <= 1 -> Difficulty.EASY
            score <= 3 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
    }

    private val TEMPERATURE = Regex(
        "\\d{2,3}\\s?°\\s?[CF]?|\\d{2,3}\\s?degrees(?:\\s?[CF](?![a-z]))?|gas mark \\d{1,2}",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Character ranges in [step] worth highlighting: times ("10 minutes", "1-2 hours") and
     * temperatures ("180°C", "350 degrees F", "gas mark 4"). Sorted and non-overlapping.
     */
    fun highlights(step: String): List<IntRange> {
        val ranges = (StepTimerParser.DURATION.findAll(step) + TEMPERATURE.findAll(step))
            .map { it.range }
            .sortedBy { it.first }
            .toList()
        val merged = mutableListOf<IntRange>()
        for (range in ranges) {
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.last) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                merged += range
            }
        }
        return merged
    }
}
