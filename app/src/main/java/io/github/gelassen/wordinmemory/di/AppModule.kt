package io.github.gelassen.wordinmemory.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.storage.AppDatabase
import javax.inject.Singleton

@Module
class AppModule(val context: Context) {

    @Provides
    fun providesStorageRepository(database: AppDatabase): StorageRepository {
        return StorageRepository(database.subjectToStudyDao())
    }

    @Provides
    @Singleton
    fun providesDatabase(): AppDatabase {
        return AppDatabase.getInstance(context)
    }
}