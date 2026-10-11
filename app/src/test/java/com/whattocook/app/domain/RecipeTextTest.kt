package com.whattocook.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextTest {

    @Test
    fun `steps drop STEP headers and numbering`() {
        val text = "STEP 1\r\nPreheat the oven.\r\n\r\nSTEP 2\r\n2. Mix for 10 minutes.\r\nStep 3: Bake for 20 minutes."
        assertEquals(
            listOf("Preheat the oven.", "Mix for 10 minutes.", "Bake for 20 minutes."),
            RecipeText.splitSteps(text),
        )
    }

    @Test
    fun `numbers inside a step are kept`() {
        assertEquals(listOf("10 minutes later, stir."), RecipeText.splitSteps("10 minutes later, stir."))
    }

    @Test
    fun `one long paragraph is split into short steps`() {
        val paragraph = (1..8).joinToString(" ") { "Sentence number $it is long enough to matter here." }
        val steps = RecipeText.splitSteps(paragraph)
        assertEquals(4, steps.size)
        assertTrue(steps.first().startsWith("Sentence number 1"))
    }

    @Test
    fun `measures split into amount and unit`() {
        assertEquals("200" to "g", RecipeText.splitMeasure("200g"))
        assertEquals("1 1/2" to "cups", RecipeText.splitMeasure("1 1/2 cups"))
        assertEquals("1½" to "cups", RecipeText.splitMeasure("1½ cups"))
        assertEquals("½" to "cup", RecipeText.splitMeasure("½ cup"))
        assertEquals("3-4" to "", RecipeText.splitMeasure("3-4"))
        assertEquals("" to "to taste", RecipeText.splitMeasure("to taste"))
        assertEquals("" to "", RecipeText.splitMeasure(""))
    }

    @Test
    fun `halal heads-up spots pork and alcohol by whole word`() {
        assertTrue(RecipeText.mayNotBeHalal(listOf("Chicken", "Smoked bacon")))
        assertTrue(RecipeText.mayNotBeHalal(listOf("Red wine")))
        assertFalse(RecipeText.mayNotBeHalal(listOf("Chicken", "Hamburger buns", "Shallots")))
    }

    @Test
    fun `halal check does not match inside other words`() {
        assertFalse(RecipeText.mayNotBeHalal(listOf("Graham crackers", "Champignons", "Rumbled potatoes")))
    }

    @Test
    fun `extracts the link from shared text`() {
        assertEquals(
            "https://www.instagram.com/reel/abc123/",
            RecipeText.extractUrl("Look at this! https://www.instagram.com/reel/abc123/ so good"),
        )
        assertEquals("https://youtu.be/xyz", RecipeText.extractUrl("https://youtu.be/xyz."))
        assertNull(RecipeText.extractUrl("no link here"))
    }

    @Test
    fun `library ids round trip`() {
        assertEquals("mealdb-52772", LibraryIds.recipeId("52772"))
        assertEquals("52772", LibraryIds.mealId("mealdb-52772"))
        assertNull(LibraryIds.mealId("3f1c-uuid"))
    }
}
