package com.whattocook.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientMatcherTest {

    private val matcher = IngredientMatcher(
        aliases = mapOf(
            "dahi" to "Yogurt",
            "haldi" to "Turmeric",
            "atta" to "Whole wheat flour",
        ),
    )

    @Test
    fun `singular and plural forms match`() {
        assertTrue(matcher.matches("Eggs", "egg"))
        assertTrue(matcher.matches("onion", "Onions"))
        assertTrue(matcher.matches("Tomatoes", "tomato"))
        assertTrue(matcher.matches("chilies", "green chili"))
    }

    @Test
    fun `quantities and descriptors are ignored`() {
        assertTrue(matcher.matches("tomato", "2 large tomatoes, chopped"))
        assertTrue(matcher.matches("garlic", "4 cloves garlic"))
        assertTrue(matcher.matches("coriander leaves", "Fresh coriander leaves, finely chopped"))
    }

    @Test
    fun `specific pantry item covers generic recipe ingredient and vice versa`() {
        assertTrue(matcher.matches("basmati rice", "rice"))
        assertTrue(matcher.matches("rice", "basmati rice"))
        assertTrue(matcher.matches("chicken", "boneless chicken thighs"))
    }

    @Test
    fun `catalog aliases are honoured`() {
        assertTrue(matcher.matches("Dahi", "plain yogurt"))
        assertTrue(matcher.matches("haldi", "Turmeric"))
        assertTrue(matcher.matches("Atta", "whole wheat flour"))
    }

    @Test
    fun `generic words only match exactly`() {
        assertFalse(matcher.matches("pepper", "red bell pepper"))
        assertFalse(matcher.matches("oil", "sesame oil"))
        assertFalse(matcher.matches("butter", "peanut butter"))
        assertTrue(matcher.matches("pepper", "pepper"))
    }

    @Test
    fun `no false positives on partial words`() {
        assertFalse(matcher.matches("egg", "eggplant"))
        assertFalse(matcher.matches("pea", "peanut"))
        assertFalse(matcher.matches("garlic", "cloves"))
    }

    @Test
    fun `cloves on its own is the spice`() {
        assertTrue(matcher.matches("Cloves", "cloves"))
        assertEquals("clove", IngredientMatcher.normalize("Cloves"))
    }

    @Test
    fun `empty input never matches`() {
        assertFalse(matcher.matches("", "salt"))
        assertFalse(matcher.matches("salt", "   "))
        assertFalse(matcher.isInPantry("salt", emptyList()))
    }

    @Test
    fun `counts how many recipe ingredients are in the pantry`() {
        val pantry = listOf("onions", "tomato", "Basmati rice", "dahi")
        val recipe = listOf("Onion", "Tomatoes", "Rice", "Yogurt", "Saffron", "Ghee")
        assertEquals(4, matcher.countInPantry(recipe, pantry))
    }

    @Test
    fun `singularize handles common English plurals`() {
        assertEquals("berry", IngredientMatcher.singularize("berries"))
        assertEquals("potato", IngredientMatcher.singularize("potatoes"))
        assertEquals("peach", IngredientMatcher.singularize("peaches"))
        assertEquals("leaf", IngredientMatcher.singularize("leaves"))
        assertEquals("hummus", IngredientMatcher.singularize("hummus"))
        assertEquals("rice", IngredientMatcher.singularize("rice"))
    }
}
