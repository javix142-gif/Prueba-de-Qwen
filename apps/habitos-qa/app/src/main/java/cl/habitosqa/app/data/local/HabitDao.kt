package cl.habitosqa.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import cl.habitosqa.app.util.HabitLogic
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC, id ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_completions ORDER BY date DESC, habitId ASC")
    fun observeCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM habits WHERE normalizedName = :normalizedName AND (:excludeId IS NULL OR id != :excludeId) LIMIT 1)")
    suspend fun nameExists(normalizedName: String, excludeId: Long?): Boolean

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun habitById(id: Long): HabitEntity?

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun completionForDay(habitId: Long, date: Long): HabitCompletionEntity?

    @Upsert
    suspend fun upsertCompletion(completion: HabitCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCompletionStrict(completion: HabitCompletionEntity)

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun completionCount(habitId: Long, date: Long): Int

    @Transaction
    suspend fun toggleCompletion(habitId: Long, date: Long) {
        val current = completionForDay(habitId, date)
        upsertCompletion(
            HabitCompletionEntity(
                habitId = habitId,
                date = date,
                completed = HabitLogic.toggledCompletion(current?.completed),
            ),
        )
    }
}
