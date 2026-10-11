package com.whattocook.app.domain

/** Helpers that turn free-text recipes (library and imports) into the app's structured form. */
object RecipeText {

    private val STEP_HEADER = Regex("^(?:step\\s*)?\\d+\\s*[.):\\-]?$", RegexOption.IGNORE_CASE)
    private val STEP_PREFIX = Regex("^(?:step\\s*\\d+\\s*[.):\\-]?|\\d+\\s*[.)]|[•\\-*])\\s*", RegexOption.IGNORE_CASE)
    private val SENTENCE_END = Regex("(?<=[.!?])\\s+(?=[A-Z0-9])")
    private val LEADING_AMOUNT =
        Regex("^(\\d+[½⅓⅔¼¾⅛]|(?:\\d+\\s+)?\\d+(?:[.,/]\\d+)?(?:\\s*[-–]\\s*\\d+(?:[.,/]\\d+)?)?|[½⅓⅔¼¾⅛](?=\\s|$))\\s*(.*)$")

    /**
     * Splits an instruction blob into steps: one per line, without "STEP 1" headers or
     * "1." prefixes. A single long paragraph is split into steps of about two sentences.
     */
    fun splitSteps(instructions: String): List<String> {
        val lines = instructions
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !STEP_HEADER.matches(it) }
            .map { it.replace(STEP_PREFIX, "").trim() }
            .filter { it.isNotEmpty() }

        if (lines.size == 1 && lines[0].length > LONG_PARAGRAPH) {
            return lines[0].split(SENTENCE_END)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .chunked(2) { it.joinToString(" ") }
        }
        return lines
    }

    /** "200g" -> ("200", "g"), "1 1/2 cups" -> ("1 1/2", "cups"), "to taste" -> ("", "to taste"). */
    fun splitMeasure(measure: String): Pair<String, String> {
        val text = measure.trim()
        val match = LEADING_AMOUNT.find(text) ?: return "" to text
        return match.groupValues[1].trim() to match.groupValues[2].trim()
    }

    private val NOT_HALAL = listOf(
        "pork", "bacon", "ham", "lard", "prosciutto", "pancetta", "chorizo", "salami", "pepperoni",
        "gammon", "wine", "beer", "rum", "brandy", "sherry", "vodka", "whisky", "whiskey", "liqueur",
        "cider", "marsala", "sake", "mirin", "kirsch", "bourbon", "cognac", "port", "stout", "ale",
    )

    /** True if any ingredient looks like pork or alcohol. A heads-up, not a guarantee. */
    fun mayNotBeHalal(ingredientNames: List<String>): Boolean = ingredientNames.any { name ->
        val words = name.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        words.any { it in NOT_HALAL }
    }

    /** Pulls the first http(s) link out of shared text like "Check this out! https://...". */
    fun extractUrl(text: String): String? =
        Regex("https?://[^\\s<>\"']+").find(text)?.value?.trimEnd('.', ',', ')', '!', '?')

    private const val LONG_PARAGRAPH = 280
}
