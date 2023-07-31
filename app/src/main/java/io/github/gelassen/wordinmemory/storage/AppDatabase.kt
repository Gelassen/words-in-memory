package io.github.gelassen.wordinmemory.storage

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.gelassen.wordinmemory.App
import io.github.gelassen.wordinmemory.storage.converters.Converters

@Database(
    version = 6,
    entities = [SubjectToStudyEntity::class],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 5, to = 6)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {

    abstract fun subjectToStudyDao(): SubjectToStudyDao

    companion object {
        // For Singleton instantiation
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, App.DATABASE_NAME)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
/*                        val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>()
                            .setInputData(workDataOf(KEY_FILENAME to PLANT_DATA_FILENAME))
                            .build()
                        WorkManager.getInstance(context).enqueue(request)*/
                        }
                    }
                )
                .build()
        }
    }
}