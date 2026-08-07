package cl.habitosqa.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HabitsDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var instance: HabitsDatabase? = null

        fun get(context: Context): HabitsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                HabitsDatabase::class.java,
                "habitos-qa.db",
            ).build().also { instance = it }
        }
    }
}
