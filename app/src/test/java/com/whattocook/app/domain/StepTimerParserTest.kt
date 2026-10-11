package com.whattocook.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTimerParserTest {

    @Test
    fun `finds a simple timer`() {
        assertEquals(listOf(StepTimer("10 minutes", 600)), StepTimerParser.parse("Simmer for 10 minutes."))
    }

    @Test
    fun `merges hours and minutes`() {
        assertEquals(listOf(StepTimer("1 hour 30 minutes", 5400)), StepTimerParser.parse("Bake for 1 hour 30 minutes"))
        assertEquals(4500, StepTimerParser.parse("Rest 1 hour and 15 minutes").single().seconds)
    }

    @Test
    fun `uses the lower bound of a range`() {
        assertEquals(listOf(StepTimer("5-7 mins", 300)), StepTimerParser.parse("Cook 5-7 mins until golden"))
    }

    @Test
    fun `keeps separate timers apart`() {
        val timers = StepTimerParser.parse("Rest 30 seconds, then fry for 2 minutes")
        assertEquals(listOf(30, 120), timers.map { it.seconds })
    }

    @Test
    fun `ignores numbers without a time unit`() {
        assertTrue(StepTimerParser.parse("Add 2 cups of water and 1 tsp salt").isEmpty())
    }
}
