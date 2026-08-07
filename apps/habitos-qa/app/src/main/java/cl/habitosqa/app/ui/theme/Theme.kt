package cl.habitosqa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF356859),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F0DB),
    onPrimaryContainer = Color(0xFF002118),
    secondary = Color(0xFF4E635B),
    surface = Color(0xFFF8FAF7),
    surfaceVariant = Color(0xFFDCE5DF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DD4BF),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF1C5041),
    onPrimaryContainer = Color(0xFFB9F0DB),
    secondary = Color(0xFFB5CCC2),
    surface = Color(0xFF101411),
    surfaceVariant = Color(0xFF404944),
)

@Composable
fun HabitosQaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
