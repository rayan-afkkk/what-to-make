package com.whattocook.app.domain

import com.whattocook.app.domain.RecipeReading.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeReadingTest {

    @Test
    fun `difficulty grows with time, steps and ingredients`() {
        assertEquals(Difficulty.EASY, RecipeReading.difficulty(stepCount = 4, ingredientCount = 6, timeMinutes = 15))
        assertEquals(Difficulty.MEDIUM, RecipeReading.difficulty(stepCount = 8, ingredientCount = 11, timeMinutes = 25))
        assertEquals(Difficulty.HARD, RecipeReading.difficulty(stepCount = 12, ingredientCount = 16, timeMinutes = 90))
    }

    @Test
    fun `highlights times and temperatures`() {
        val step = "Bake at 180°C for 20 minutes, then rest 5-7 mins."
        val words = RecipeReading.highlights(step).map { step.substring(it) }
        assertEquals(listOf("180°C", "20 minutes", "5-7 mins"), words)
    }

    @Test
    fun `highlights degrees and gas mark`() {
        val step = "Heat the oven to 350 degrees F or gas mark 4."
        val words = RecipeReading.highlights(step).map { step.substring(it) }
        assertEquals(listOf("350 degrees F", "gas mark 4"), words)
    }

    @Test
    fun `plain steps have nothing highlighted`() {
        assertTrue(RecipeReading.highlights("Chop the onion finely.").isEmpty())
    }
}
