package cl.habitosqa.app.data.repository

import cl.habitosqa.app.model.Habit
import cl.habitosqa.app.model.HabitCompletion
import cl.habitosqa.app.model.SaveResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRepositoryTest {
    private val store = FakeHabitStore()
    private val repository = HabitRepository(store)

    @Test
    fun emptyNameIsRejected() = runTest {
        val result = repository.createHabit("   ", 100)
        assertTrue(result is SaveResult.Error)
        assertTrue(store.habits.value.isEmpty())
    }

    @Test
    fun nameIsTrimmedBeforeSaving() = runTest {
        assertEquals(SaveResult.Success, repository.createHabit("  Leer  ", 100))
        assertEquals("Leer", store.habits.value.single().name)
    }

    @Test
    fun nameAllowsFortyCharactersAndRejectsFortyOne() = runTest {
        val forty = "a".repeat(40)
        assertEquals(SaveResult.Success, repository.createHabit(forty, 100))
        assertTrue(repository.createHabit("b".repeat(41), 100) is SaveResult.Error)
    }

    @Test
    fun duplicateIgnoresCaseAndOuterSpaces() = runTest {
        assertEquals(SaveResult.Success, repository.createHabit("Leer", 100))
        val duplicate = repository.createHabit(" leer ", 100)
        assertTrue(duplicate is SaveResult.Error)
        assertEquals(1, store.habits.value.size)
    }

    @Test
    fun pendingTogglesToCompleted() = runTest {
        repository.createHabit("Agua", 100)
        val id = store.habits.value.single().id
        repository.toggleCompletion(id, 100)
        assertTrue(store.completions.value.single().completed)
    }

    @Test
    fun completedTogglesBackToPending() = runTest {
        repository.createHabit("Agua", 100)
        val id = store.habits.value.single().id
        repository.toggleCompletion(id, 100)
        repository.toggleCompletion(id, 100)
        assertFalse(store.completions.value.single().completed)
    }

    @Test
    fun togglingDoesNotDuplicateLogicalCompletion() = runTest {
        repository.createHabit("Agua", 100)
        val id = store.habits.value.single().id
        repeat(3) { repository.toggleCompletion(id, 100) }
        assertEquals(1, store.completions.value.count { it.habitId == id && it.date == 100L })
    }

    @Test
    fun editingPreservesIdCreationAndHistory() = runTest {
        repository.createHabit("Leer 20 min", 100)
        val original = store.habits.value.single()
        repository.toggleCompletion(original.id, 100)

        assertEquals(SaveResult.Success, repository.editHabit(original.id, "Leer 30 min"))
        val edited = store.habits.value.single()
        assertEquals(original.id, edited.id)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals("Leer 30 min", edited.name)
        assertEquals(1, store.completions.value.size)
        assertEquals(original.id, store.completions.value.single().habitId)
    }

    @Test
    fun deletingHabitDeletesItsHistory() = runTest {
        repository.createHabit("Agua", 100)
        val id = store.habits.value.single().id
        repository.toggleCompletion(id, 100)
        repository.deleteHabit(id)
        assertTrue(store.habits.value.isEmpty())
        assertTrue(store.completions.value.isEmpty())
    }

    private class FakeHabitStore : HabitStore {
        val habits = MutableStateFlow<List<Habit>>(emptyList())
        val completions = MutableStateFlow<List<HabitCompletion>>(emptyList())
        private val normalizedNames = mutableMapOf<Long, String>()
        private var nextId = 1L

        override fun observeHabits(): Flow<List<Habit>> = habits
        override fun observeCompletions(): Flow<List<HabitCompletion>> = completions

        override suspend fun nameExists(normalizedName: String, excludeId: Long?): Boolean =
            normalizedNames.any { (id, value) -> id != excludeId && value == normalizedName }

        override suspend fun insertHabit(name: String, normalizedName: String, createdAt: Long): Long {
            val id = nextId++
            habits.value = habits.value + Habit(id, name, createdAt)
            normalizedNames[id] = normalizedName
            return id
        }

        override suspend fun habitById(id: Long): Habit? = habits.value.firstOrNull { it.id == id }

        override suspend fun updateHabit(habit: Habit, normalizedName: String) {
            habits.value = habits.value.map { if (it.id == habit.id) habit else it }
            normalizedNames[habit.id] = normalizedName
        }

        override suspend fun deleteHabit(id: Long) {
            habits.value = habits.value.filterNot { it.id == id }
            completions.value = completions.value.filterNot { it.habitId == id }
            normalizedNames.remove(id)
        }

        override suspend fun toggleCompletion(habitId: Long, date: Long) {
            val current = completions.value.firstOrNull { it.habitId == habitId && it.date == date }
            val replacement = HabitCompletion(habitId, date, !(current?.completed ?: false))
            completions.value = completions.value.filterNot { it.habitId == habitId && it.date == date } + replacement
        }
    }
}
