package cl.habitosqa.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitLogicTest {
    @Test
    fun progressZeroOfZero() {
        assertEquals("0 de 0 completados", HabitLogic.progressText(0, 0))
    }

    @Test
    fun progressPartial() {
        assertEquals("2 de 4 completados", HabitLogic.progressText(4, 2))
    }

    @Test
    fun progressComplete() {
        assertEquals("4 de 4 completados", HabitLogic.progressText(4, 4))
    }

    @Test
    fun streakIsZeroWhenTodayIsPending() {
        assertEquals(0, HabitLogic.streak(100, setOf(99, 98)))
    }

    @Test
    fun streakIsOneWhenOnlyTodayIsComplete() {
        assertEquals(1, HabitLogic.streak(100, setOf(100, 98)))
    }

    @Test
    fun streakCountsSeveralConsecutiveDays() {
        assertEquals(4, HabitLogic.streak(100, setOf(100, 99, 98, 97, 95)))
    }

    @Test
    fun streakStopsAtFirstGap() {
        assertEquals(2, HabitLogic.streak(100, setOf(100, 99, 97, 96)))
    }

    @Test
    fun lastSevenDaysContainsTodayAndSixPreviousDays() {
        assertEquals(listOf(100L, 99L, 98L, 97L, 96L, 95L, 94L), HabitLogic.lastSevenDays(100))
    }
}
