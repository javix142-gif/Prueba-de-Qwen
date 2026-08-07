package cl.habitosqa.app.util

import java.util.Locale

enum class HistoryDayState {
    COMPLETED,
    PENDING,
    BEFORE_CREATION,
}

object HabitLogic {
    const val MAX_NAME_LENGTH = 40

    fun cleanName(raw: String): String = raw.trim()

    fun normalizedName(raw: String): String = cleanName(raw).lowercase(Locale.ROOT)

    fun nameError(raw: String): String? {
        val clean = cleanName(raw)
        return when {
            clean.isEmpty() -> "Ingresa un nombre para el hábito."
            clean.length > MAX_NAME_LENGTH -> "El nombre puede tener hasta 40 caracteres."
            else -> null
        }
    }

    fun progressText(total: Int, completed: Int): String = "$completed de $total completados"

    fun toggledCompletion(current: Boolean?): Boolean = !(current ?: false)

    fun streak(today: Long, completedDays: Set<Long>): Int {
        if (today !in completedDays) return 0
        var day = today
        var result = 0
        while (day in completedDays) {
            result += 1
            day -= 1
        }
        return result
    }

    fun lastSevenDays(today: Long): List<Long> = (0L..6L).map { today - it }

    fun historyState(createdAt: Long, day: Long, completedDays: Set<Long>): HistoryDayState = when {
        day < createdAt -> HistoryDayState.BEFORE_CREATION
        day in completedDays -> HistoryDayState.COMPLETED
        else -> HistoryDayState.PENDING
    }
}
