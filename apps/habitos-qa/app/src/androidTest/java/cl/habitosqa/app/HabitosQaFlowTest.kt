package cl.habitosqa.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.habitosqa.app.data.local.HabitsDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitosQaFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        runBlocking {
            HabitsDatabase.get(InstrumentationRegistry.getInstrumentation().targetContext).clearAllTables()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun flowAEmptyState() {
        composeRule.onNodeWithText("Aún no tienes hábitos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Agregar hábito").assertIsDisplayed()
    }

    @Test
    fun flowsBCDEFGCrudProgressDuplicateHistoryAndDelete() {
        addHabit("Beber agua")
        addHabit("Leer 20 min")
        composeRule.onNodeWithText("0 de 2 completados").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Marcar Beber agua").performClick()
        composeRule.onNodeWithText("1 de 2 completados").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Desmarcar Beber agua").performClick()
        composeRule.onNodeWithText("0 de 2 completados").assertIsDisplayed()

        openOptions("Leer 20 min")
        composeRule.onNodeWithText("Editar").performClick()
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("Leer 30 min")
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.onNodeWithText("Leer 30 min").assertIsDisplayed()
        composeRule.onNodeWithText("Leer 20 min").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Agregar hábito").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(" leer 30 min ")
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.onNodeWithText("Ya existe un hábito con ese nombre.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.onNodeWithContentDescription("Marcar Beber agua").performClick()
        composeRule.onNodeWithText("Historial").performClick()
        composeRule.onNodeWithText("Últimos 7 días").assertIsDisplayed()
        composeRule.onNodeWithText("Beber agua").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Completado").assertCountEquals(1)
        composeRule.onNodeWithText("Hoy").performClick()

        openOptions("Leer 30 min")
        composeRule.onNodeWithText("Eliminar").performClick()
        composeRule.onNodeWithText("¿Eliminar hábito?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Leer 30 min").assertIsDisplayed()

        openOptions("Leer 30 min")
        composeRule.onNodeWithText("Eliminar").performClick()
        composeRule.onNodeWithText("Eliminar").performClick()
        composeRule.onNodeWithText("Leer 30 min").assertDoesNotExist()
    }

    @Test
    fun flowHPersistsAcrossActivityRecreation() {
        addHabit("Persistente")
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithText("Persistente").assertIsDisplayed()
    }

    private fun addHabit(name: String) {
        composeRule.onNodeWithContentDescription("Agregar hábito").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(name)
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openOptions(name: String) {
        composeRule.onNode(hasContentDescription("Opciones de $name")).performClick()
    }
}
