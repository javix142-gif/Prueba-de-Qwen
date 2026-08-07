package cl.habitosqa.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.habitosqa.app.data.local.HabitsDatabase
import cl.habitosqa.app.data.local.RoomHabitStore
import cl.habitosqa.app.data.repository.HabitRepository
import cl.habitosqa.app.model.Habit
import cl.habitosqa.app.model.HabitCompletion
import cl.habitosqa.app.model.SaveResult
import cl.habitosqa.app.util.HabitLogic
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val today: Long = LocalDate.now().toEpochDay(),
) {
    val completedToday: Int
        get() = completions.count { it.date == today && it.completed }

    val progressText: String
        get() = HabitLogic.progressText(habits.size, completedToday)

    fun isCompletedToday(habitId: Long): Boolean =
        completions.any { it.habitId == habitId && it.date == today && it.completed }

    fun streak(habitId: Long): Int = HabitLogic.streak(
        today = today,
        completedDays = completions.asSequence()
            .filter { it.habitId == habitId && it.completed }
            .map { it.date }
            .toSet(),
    )
}

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HabitRepository(RoomHabitStore(HabitsDatabase.get(application)))
    private val today = kotlinx.coroutines.flow.MutableStateFlow(LocalDate.now().toEpochDay())

    val uiState: StateFlow<HabitsUiState> = combine(
        repository.observeHabits(),
        repository.observeCompletions(),
        today,
    ) { habits, completions, currentDay ->
        HabitsUiState(habits = habits, completions = completions, today = currentDay)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState(),
    )

    fun refreshDate() {
        today.value = LocalDate.now().toEpochDay()
    }

    suspend fun saveHabit(name: String, editingId: Long?): String? {
        val result = if (editingId == null) {
            repository.createHabit(name, today.value)
        } else {
            repository.editHabit(editingId, name)
        }
        return (result as? SaveResult.Error)?.message
    }

    fun toggle(habitId: Long) {
        viewModelScope.launch { repository.toggleCompletion(habitId, today.value) }
    }

    fun delete(habitId: Long) {
        viewModelScope.launch { repository.deleteHabit(habitId) }
    }
}
