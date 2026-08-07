package cl.habitosqa.app.data.repository

import cl.habitosqa.app.model.Habit
import cl.habitosqa.app.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitStore {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCompletions(): Flow<List<HabitCompletion>>
    suspend fun nameExists(normalizedName: String, excludeId: Long? = null): Boolean
    suspend fun insertHabit(name: String, normalizedName: String, createdAt: Long): Long
    suspend fun habitById(id: Long): Habit?
    suspend fun updateHabit(habit: Habit, normalizedName: String)
    suspend fun deleteHabit(id: Long)
    suspend fun toggleCompletion(habitId: Long, date: Long)
}
