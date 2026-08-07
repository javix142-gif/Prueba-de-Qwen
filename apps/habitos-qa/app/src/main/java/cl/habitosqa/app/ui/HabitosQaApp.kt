package cl.habitosqa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.habitosqa.app.model.Habit
import cl.habitosqa.app.util.HabitLogic
import cl.habitosqa.app.util.HistoryDayState
import cl.habitosqa.app.viewmodel.HabitsUiState
import cl.habitosqa.app.viewmodel.HabitsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private enum class Destination { TODAY, HISTORY }
private val ChileanLocale = Locale("es", "CL")

@Composable
fun HabitosQaApp(viewModel: HabitsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var destinationName by rememberSaveable { mutableStateOf(Destination.TODAY.name) }
    val destination = Destination.valueOf(destinationName)
    var editorHabit by remember { mutableStateOf<Habit?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Habit?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshDate() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hábitos QA") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == Destination.TODAY,
                    onClick = { destinationName = Destination.TODAY.name },
                    icon = { Icon(Icons.Default.Today, contentDescription = null) },
                    label = { Text("Hoy") },
                )
                NavigationBarItem(
                    selected = destination == Destination.HISTORY,
                    onClick = { destinationName = Destination.HISTORY.name },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Historial") },
                )
            }
        },
        floatingActionButton = {
            if (destination == Destination.TODAY) {
                FloatingActionButton(
                    onClick = {
                        editorHabit = null
                        editorOpen = true
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar hábito")
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.TODAY -> TodayScreen(
                uiState = uiState,
                padding = padding,
                onToggle = viewModel::toggle,
                onEdit = {
                    editorHabit = it
                    editorOpen = true
                },
                onDelete = { deleteTarget = it },
            )
            Destination.HISTORY -> HistoryScreen(uiState = uiState, padding = padding)
        }
    }

    if (editorOpen) {
        HabitEditorDialog(
            habit = editorHabit,
            onDismiss = { editorOpen = false },
            onSave = viewModel::saveHabit,
            onSaved = { editorOpen = false },
        )
    }

    deleteTarget?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("¿Eliminar hábito?") },
            text = { Text("Se eliminará también su historial.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(habit.id)
                    deleteTarget = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun TodayScreen(
    uiState: HabitsUiState,
    padding: PaddingValues,
    onToggle: (Long) -> Unit,
    onEdit: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = LocalDate.ofEpochDay(uiState.today).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ChileanLocale)).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = uiState.progressText,
            modifier = Modifier.semantics { contentDescription = "Progreso diario: ${uiState.progressText}" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(12.dp))

        if (uiState.habits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Aún no tienes hábitos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Agrega el primero para comenzar.", color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        completed = uiState.isCompletedToday(habit.id),
                        streak = uiState.streak(habit.id),
                        onToggle = { onToggle(habit.id) },
                        onEdit = { onEdit(habit) },
                        onDelete = { onDelete(habit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    completed: Boolean,
    streak: Int,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics {
                    contentDescription = if (completed) "Desmarcar ${habit.name}" else "Marcar ${habit.name}"
                },
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (streak == 1) "Racha: 1 día" else "Racha: $streak días",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones de ${habit.name}")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(uiState: HabitsUiState, padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Últimos 7 días", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        if (uiState.habits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no tienes hábitos")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.habits, key = { it.id }) { habit ->
                    HistoryHabitCard(habit = habit, uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun HistoryHabitCard(habit: Habit, uiState: HabitsUiState) {
    val days = HabitLogic.lastSevenDays(uiState.today)
    val completedDays = uiState.completions.asSequence()
        .filter { it.habitId == habit.id && it.completed }
        .map { it.date }
        .toSet()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(habit.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                days.forEachIndexed { index, day ->
                    val date = LocalDate.ofEpochDay(day)
                    val state = HabitLogic.historyState(habit.createdAt, day, completedDays)
                    val statusLabel = when (state) {
                        HistoryDayState.COMPLETED -> "Completado"
                        HistoryDayState.PENDING -> "Pendiente"
                        HistoryDayState.BEFORE_CREATION -> "Anterior a creación"
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = statusLabel },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (index == 0) "Hoy" else date.dayOfWeek.getDisplayName(TextStyle.NARROW, ChileanLocale).uppercase(ChileanLocale),
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                        Text(text = date.dayOfMonth.toString(), fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        val symbol = when (state) {
                            HistoryDayState.COMPLETED -> "✓"
                            HistoryDayState.PENDING -> "○"
                            HistoryDayState.BEFORE_CREATION -> "—"
                        }
                        val background = when (state) {
                            HistoryDayState.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            HistoryDayState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                            HistoryDayState.BEFORE_CREATION -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(background),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(symbol, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitEditorDialog(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: suspend (String, Long?) -> String?,
    onSaved: () -> Unit,
) {
    var name by remember(habit?.id) { mutableStateOf(habit?.name.orEmpty()) }
    var error by remember(habit?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (habit == null) "Agregar hábito" else "Editar hábito") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> ({ Text(message) }) },
                )
                Text(
                    text = "${HabitLogic.cleanName(name).length}/${HabitLogic.MAX_NAME_LENGTH}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    error = onSave(name, habit?.id)
                    if (error == null) onSaved()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
