package cl.habitosqa.app.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.habitosqa.app.model.Habit
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomHabitStoreTest {
    private lateinit var database: HabitsDatabase
    private lateinit var dao: HabitDao
    private lateinit var store: RoomHabitStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitsDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.habitDao()
        store = RoomHabitStore(database, dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertReadUpdateAndDeleteHabit() = runBlocking {
        val id = store.insertHabit("Leer", "leer", 100)
        assertEquals("Leer", store.observeHabits().first().single().name)

        store.updateHabit(Habit(id, "Leer 30 min", 100), "leer 30 min")
        assertEquals("Leer 30 min", store.observeHabits().first().single().name)

        store.deleteHabit(id)
        assertTrue(store.observeHabits().first().isEmpty())
    }

    @Test
    fun completionPrimaryKeyEnforcesHabitAndDateUniqueness() = runBlocking {
        val id = store.insertHabit("Agua", "agua", 100)
        dao.insertCompletionStrict(HabitCompletionEntity(id, 100, true))
        var constrained = false
        try {
            dao.insertCompletionStrict(HabitCompletionEntity(id, 100, false))
        } catch (_: SQLiteConstraintException) {
            constrained = true
        }
        assertTrue(constrained)
        assertEquals(1, dao.completionCount(id, 100))
    }

    @Test
    fun deletingHabitCascadesToHistory() = runBlocking {
        val id = store.insertHabit("Agua", "agua", 100)
        store.toggleCompletion(id, 100)
        assertEquals(1, dao.completionCount(id, 100))
        store.deleteHabit(id)
        assertEquals(0, dao.completionCount(id, 100))
    }

    @Test
    fun flowEmitsAfterInsert() = runBlocking {
        val emission = async {
            store.observeHabits().filter { it.isNotEmpty() }.first()
        }
        store.insertHabit("Caminar", "caminar", 100)
        assertEquals("Caminar", emission.await().single().name)
    }
}
