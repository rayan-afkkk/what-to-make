package com.whattocook.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServingScalerTest {

    @Test
    fun `scales whole numbers`() {
        assertEquals("4", ServingScaler.scale("2", fromServings = 2, toServings = 4))
        assertEquals("1", ServingScaler.scale("2", fromServings = 4, toServings = 2))
    }

    @Test
    fun `scales fractions and mixed numbers`() {
        assertEquals("1", ServingScaler.scale("1/2", 2, 4))
        assertEquals("3", ServingScaler.scale("1 1/2", 2, 4))
        assertEquals("1 1/2", ServingScaler.scale("1", 2, 3))
        assertEquals("2/3", ServingScaler.scale("1", 3, 2))
        assertEquals("1/4", ServingScaler.scale("1", 4, 1))
        assertEquals("3/4", ServingScaler.scale("0.25", 1, 3))
    }

    @Test
    fun `understands unicode fractions`() {
        assertEquals("1", ServingScaler.scale("½", 1, 2))
        assertEquals("3", ServingScaler.scale("1½", 1, 2))
    }

    @Test
    fun `large amounts are rounded to one decimal`() {
        assertEquals("300", ServingScaler.scale("200", 4, 6))
        assertEquals("187.5", ServingScaler.scale("250", 4, 3))
    }

    @Test
    fun `scales ranges`() {
        assertEquals("4-6", ServingScaler.scale("2-3", 2, 4))
    }

    @Test
    fun `leaves non numeric quantities alone`() {
        assertEquals("to taste", ServingScaler.scale("to taste", 2, 4))
        assertEquals("a pinch", ServingScaler.scale("a pinch", 2, 6))
        assertEquals("", ServingScaler.scale("", 2, 4))
    }

    @Test
    fun `same or invalid serving counts return the input`() {
        assertEquals("1½", ServingScaler.scale("1½", 2, 2))
        assertEquals("2", ServingScaler.scale("2", 0, 4))
        assertEquals("2", ServingScaler.scale("2", 4, 0))
    }

    @Test
    fun `parse handles decimals and commas`() {
        assertEquals(1.5, ServingScaler.parse("1.5")!!, 1e-9)
        assertEquals(1.5, ServingScaler.parse("1,5")!!, 1e-9)
        assertEquals(0.25, ServingScaler.parse("1/4")!!, 1e-9)
        assertNull(ServingScaler.parse("1/0"))
        assertNull(ServingScaler.parse("some"))
    }

    @Test
    fun `format prefers kitchen fractions and falls back to decimals`() {
        assertEquals("1/3", ServingScaler.format(1.0 / 3))
        assertEquals("2 1/2", ServingScaler.format(2.5))
        assertEquals("0.17", ServingScaler.format(1.0 / 6))
        assertEquals("10", ServingScaler.format(9.98))
    }
}
