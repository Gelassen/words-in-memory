package io.github.gelassen.wordinmemory.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.backgroundjobs.MyWorkerFactory
import io.github.gelassen.wordinmemory.di.AppModule.Consts.DISPATCHER_IO
import io.github.gelassen.wordinmemory.ml.PlainTranslator
import io.github.gelassen.wordinmemory.network.DynamicBaseUrlInterceptor
import io.github.gelassen.wordinmemory.network.IApi
import io.github.gelassen.wordinmemory.repository.NetworkRepository
import io.github.gelassen.wordinmemory.repository.StorageRepository
import io.github.gelassen.wordinmemory.storage.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class AppModule(val application: Application) {

    object Consts {
        const val DISPATCHER_IO = "DISPATCHER_IO"
    }

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

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(sharedPreferences: SharedPreferences): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        return OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlInterceptor(sharedPreferences))
            .addInterceptor(logging)
            .build()
    }

    @Singleton
    @Provides
    fun provideApi(httpCLinet: OkHttpClient): IApi {
        val url = application.getString(R.string.endpoint)
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpCLinet)
            .baseUrl(url)
            .build()

        return retrofit.create(IApi::class.java)
    }

    @Provides
    fun provideNetworkRepository(api: IApi): NetworkRepository {
        return NetworkRepository(api)
    }

    @Provides
    @Named(DISPATCHER_IO)
    fun providesNetworkDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    fun providesMyWorkerFactory(
        translator: PlainTranslator,
        storageRepository: StorageRepository,
        networkRepository: NetworkRepository,
        @Named(DISPATCHER_IO) dispatcher: CoroutineDispatcher
    ): MyWorkerFactory {
        return MyWorkerFactory(
            translator = translator,
            storageRepository = storageRepository,
            networkRepository = networkRepository,
            backgroundDispatcher = dispatcher
        )
    }

    // leave this object without annotation, his lifecycle is app's lifetime
    @Provides
    fun provideTranslator(): PlainTranslator {
        return PlainTranslator(null)
    }

}