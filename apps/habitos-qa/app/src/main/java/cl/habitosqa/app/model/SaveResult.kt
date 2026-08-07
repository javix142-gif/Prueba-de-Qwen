package cl.habitosqa.app.model

sealed interface SaveResult {
    data object Success : SaveResult
    data class Error(val message: String) : SaveResult
}
