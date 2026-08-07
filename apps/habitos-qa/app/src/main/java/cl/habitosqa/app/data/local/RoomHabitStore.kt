package cl.habitosqa.app.data.local

import androidx.room.withTransaction
import cl.habitosqa.app.data.repository.HabitStore
import cl.habitosqa.app.model.Habit
import cl.habitosqa.app.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHabitStore(
    private val database: HabitsDatabase,
    private val dao: HabitDao = database.habitDao(),
) : HabitStore {
    override fun observeHabits(): Flow<List<Habit>> = dao.observeHabits().map { list -> list.map { it.toModel() } }

    override fun observeCompletions(): Flow<List<HabitCompletion>> = dao.observeCompletions().map { list -> list.map { it.toModel() } }

    override suspend fun nameExists(normalizedName: String, excludeId: Long?): Boolean =
        dao.nameExists(normalizedName, excludeId)

    override suspend fun insertHabit(name: String, normalizedName: String, createdAt: Long): Long =
        dao.insertHabit(HabitEntity(name = name, normalizedName = normalizedName, createdAt = createdAt))

    override suspend fun habitById(id: Long): Habit? = dao.habitById(id)?.toModel()

    override suspend fun updateHabit(habit: Habit, normalizedName: String) {
        dao.updateHabit(HabitEntity(habit.id, habit.name, normalizedName, habit.createdAt))
    }

    override suspend fun deleteHabit(id: Long) {
        database.withTransaction { dao.deleteHabit(id) }
    }

    override suspend fun toggleCompletion(habitId: Long, date: Long) {
        database.withTransaction { dao.toggleCompletion(habitId, date) }
    }

    private fun HabitEntity.toModel() = Habit(id = id, name = name, createdAt = createdAt)
    private fun HabitCompletionEntity.toModel() = HabitCompletion(habitId = habitId, date = date, completed = completed)
}
