package com.whattocook.app.domain

/** A timer found inside a recipe step, e.g. "simmer for 10 minutes". */
data class StepTimer(
    val label: String,
    val seconds: Int,
)

object StepTimerParser {

    internal val DURATION = Regex(
        "(\\d+(?:\\.\\d+)?)(?:\\s*(?:-|–|to)\\s*(\\d+(?:\\.\\d+)?))?\\s*" +
            "(hours?|hrs?|minutes?|mins?|seconds?|secs?)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val JOINER = Regex("^\\s*(?:and\\s*)?$", RegexOption.IGNORE_CASE)

    fun parse(step: String): List<StepTimer> {
        val matches = DURATION.findAll(step).toList()
        val timers = mutableListOf<StepTimer>()
        var index = 0
        while (index < matches.size) {
            val match = matches[index]
            var seconds = secondsOf(match)
            var end = match.range.last + 1
            // Merge "1 hour 30 minutes" / "1 hour and 30 minutes" into one timer.
            while (index + 1 < matches.size) {
                val next = matches[index + 1]
                val gap = step.substring(end, next.range.first)
                if (!JOINER.matches(gap) || unitRank(next) >= unitRank(matches[index])) break
                seconds += secondsOf(next)
                end = next.range.last + 1
                index++
            }
            if (seconds > 0) {
                timers += StepTimer(label = step.substring(match.range.first, end).trim(), seconds = seconds)
            }
            index++
        }
        return timers
    }

    private fun secondsOf(match: MatchResult): Int {
        // For ranges ("5-7 minutes") start with the lower bound; the cook can add time.
        val amount = match.groupValues[1].toDoubleOrNull() ?: return 0
        val unitSeconds = when (unitRank(match)) {
            3 -> 3600
            2 -> 60
            else -> 1
        }
        return (amount * unitSeconds).toInt()
    }

    private fun unitRank(match: MatchResult): Int {
        val unit = match.groupValues[3].lowercase()
        return when {
            unit.startsWith("h") -> 3
            unit.startsWith("m") -> 2
            else -> 1
        }
    }
}
