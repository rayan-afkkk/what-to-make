package com.whattocook.app.domain

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Rescales ingredient quantities when the user changes the number of servings.
 * Understands "2", "1.5", "1/2", "1 1/2", "1½", "½" and ranges like "2-3".
 * Anything it cannot parse ("to taste", "a pinch") is returned unchanged.
 */
object ServingScaler {

    private val UNICODE_FRACTIONS = mapOf(
        '½' to "1/2", '⅓' to "1/3", '⅔' to "2/3", '¼' to "1/4", '¾' to "3/4",
        '⅛' to "1/8", '⅜' to "3/8", '⅝' to "5/8", '⅞' to "7/8",
    )

    private val NICE_FRACTIONS = listOf(
        0.0 to "", 0.125 to "1/8", 0.25 to "1/4", 1.0 / 3 to "1/3", 0.5 to "1/2",
        2.0 / 3 to "2/3", 0.75 to "3/4", 1.0 to "",
    )

    private val RANGE = Regex("^(.+?)\\s*(?:-|–|—|to)\\s*(.+)$")

    fun scale(quantity: String, fromServings: Int, toServings: Int): String {
        val trimmed = quantity.trim()
        if (trimmed.isEmpty() || fromServings <= 0 || toServings <= 0 || fromServings == toServings) {
            return trimmed
        }
        val factor = toServings.toDouble() / fromServings

        parse(trimmed)?.let { return format(it * factor) }

        RANGE.matchEntire(trimmed)?.let { match ->
            val low = parse(match.groupValues[1])
            val high = parse(match.groupValues[2])
            if (low != null && high != null) {
                return "${format(low * factor)}-${format(high * factor)}"
            }
        }
        return trimmed
    }

    /** Parses a quantity string into a number, or null if it is not numeric. */
    fun parse(quantity: String): Double? {
        var text = quantity.trim()
        UNICODE_FRACTIONS.forEach { (symbol, ascii) -> text = text.replace(symbol.toString(), " $ascii") }
        val parts = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (parts.size) {
            1 -> parseSingle(parts[0])
            2 -> {
                val whole = parts[0].toDoubleOrNull()
                val fraction = if (parts[1].contains('/')) parseSingle(parts[1]) else null
                if (whole != null && fraction != null && whole == floor(whole)) whole + fraction else null
            }
            else -> null
        }
    }

    private fun parseSingle(token: String): Double? {
        if (token.contains('/')) {
            val pieces = token.split('/')
            if (pieces.size != 2) return null
            val numerator = pieces[0].toDoubleOrNull() ?: return null
            val denominator = pieces[1].toDoubleOrNull() ?: return null
            return if (denominator == 0.0) null else numerator / denominator
        }
        return token.replace(',', '.').toDoubleOrNull()
    }

    /** Formats a number as a friendly kitchen quantity ("1 1/2", "1/3", "250", "2.5"). */
    fun format(value: Double): String {
        if (value <= 0.0) return "0"
        if (value >= 10) {
            val rounded = (value * 10).roundToInt() / 10.0
            return trimZeros(rounded)
        }
        var whole = floor(value).toInt()
        val fraction = value - whole
        val nice = NICE_FRACTIONS.minByOrNull { abs(it.first - fraction) }!!
        if (abs(nice.first - fraction) <= 0.04) {
            if (nice.first == 1.0) whole += 1
            return when {
                nice.second.isEmpty() -> whole.toString()
                whole == 0 -> nice.second
                else -> "$whole ${nice.second}"
            }
        }
        return trimZeros((value * 100).roundToInt() / 100.0)
    }

    private fun trimZeros(value: Double): String {
        val text = value.toString()
        return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
    }
}
