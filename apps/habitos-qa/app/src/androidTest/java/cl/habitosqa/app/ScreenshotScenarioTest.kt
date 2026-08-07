package cl.habitosqa.app

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import cl.habitosqa.app.data.local.HabitsDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenshotScenarioTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    @Before
    fun prepare() {
        device.executeShellCommand("cmd uimode night no")
        device.executeShellCommand("settings put system font_scale 1.0")
        runBlocking { HabitsDatabase.get(instrumentation.targetContext).clearAllTables() }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @After
    fun restore() {
        device.executeShellCommand("cmd uimode night no")
        device.executeShellCommand("settings put system font_scale 1.0")
    }

    @Test
    fun captureEmptyLight() {
        composeRule.onNodeWithText("Aún no tienes hábitos").assertExists()
        capture("01_empty_light.png")
    }

    @Test
    fun captureHabitsLight() {
        seedHabits()
        capture("02_habits_light.png")
    }

    @Test
    fun captureHistoryLight() {
        seedHabits()
        composeRule.onNodeWithText("Historial").performClick()
        composeRule.onNodeWithText("Últimos 7 días").assertExists()
        capture("03_history_light.png")
    }

    @Test
    fun captureHabitsDark() {
        seedHabits()
        device.executeShellCommand("cmd uimode night yes")
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        capture("04_habits_dark.png")
    }

    @Test
    fun captureLargeText() {
        seedHabits()
        device.executeShellCommand("settings put system font_scale 1.5")
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        capture("05_large_text.png")
    }

    private fun seedHabits() {
        addHabit("Beber agua")
        addHabit("Leer 30 min")
        composeRule.onNodeWithContentDescription("Marcar Beber agua").performClick()
        composeRule.onNodeWithText("1 de 2 completados").assertExists()
    }

    private fun addHabit(name: String) {
        composeRule.onNodeWithContentDescription("Agregar hábito").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(name)
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.onNodeWithText(name).assertExists()
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        assertTrue("No se pudo guardar $name", device.takeScreenshot(File(dir, name)))
    }
}
