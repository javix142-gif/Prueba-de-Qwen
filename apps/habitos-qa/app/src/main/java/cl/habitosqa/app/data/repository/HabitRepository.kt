package cl.habitosqa.app.data.repository

import cl.habitosqa.app.model.SaveResult
import cl.habitosqa.app.util.HabitLogic
import kotlinx.coroutines.flow.Flow
import cl.habitosqa.app.model.Habit
import cl.habitosqa.app.model.HabitCompletion

class HabitRepository(private val store: HabitStore) {
    fun observeHabits(): Flow<List<Habit>> = store.observeHabits()
    fun observeCompletions(): Flow<List<HabitCompletion>> = store.observeCompletions()

    suspend fun createHabit(rawName: String, createdAt: Long): SaveResult {
        HabitLogic.nameError(rawName)?.let { return SaveResult.Error(it) }
        val clean = HabitLogic.cleanName(rawName)
        val normalized = HabitLogic.normalizedName(rawName)
        if (store.nameExists(normalized)) {
            return SaveResult.Error("Ya existe un hábito con ese nombre.")
        }
        store.insertHabit(clean, normalized, createdAt)
        return SaveResult.Success
    }

    suspend fun editHabit(id: Long, rawName: String): SaveResult {
        HabitLogic.nameError(rawName)?.let { return SaveResult.Error(it) }
        val current = store.habitById(id) ?: return SaveResult.Error("El hábito ya no existe.")
        val normalized = HabitLogic.normalizedName(rawName)
        if (store.nameExists(normalized, excludeId = id)) {
            return SaveResult.Error("Ya existe un hábito con ese nombre.")
        }
        store.updateHabit(current.copy(name = HabitLogic.cleanName(rawName)), normalized)
        return SaveResult.Success
    }

    suspend fun deleteHabit(id: Long) = store.deleteHabit(id)

    suspend fun toggleCompletion(habitId: Long, date: Long) = store.toggleCompletion(habitId, date)
}
