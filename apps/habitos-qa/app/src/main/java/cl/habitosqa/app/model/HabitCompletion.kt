package cl.habitosqa.app.model

data class HabitCompletion(
    val habitId: Long,
    val date: Long,
    val completed: Boolean,
)
