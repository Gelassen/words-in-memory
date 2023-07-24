package io.github.gelassen.wordinmemory.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.storage.AppDatabase
import javax.inject.Singleton

@Module
class AppModule(val application: Application) {

    @Provides
    fun providesStorageRepository(database: AppDatabase): StorageRepository {
        return StorageRepository(database.subjectToStudyDao())
    }

    @Provides
    @Singleton
    fun providesDatabase(): AppDatabase {
        return AppDatabase.getInstance(application)
    }

    @Provides
    fun provideApplication(): Application {
        return application
    }

    @Provides
    fun provideContext(): Context {
        return application
    }
}