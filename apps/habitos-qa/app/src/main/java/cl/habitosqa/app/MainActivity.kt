package cl.habitosqa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.habitosqa.app.ui.HabitosQaApp
import cl.habitosqa.app.ui.theme.HabitosQaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitosQaTheme {
                HabitosQaApp()
            }
        }
    }
}
