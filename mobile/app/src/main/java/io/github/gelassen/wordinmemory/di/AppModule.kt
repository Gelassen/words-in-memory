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
import io.github.gelassen.wordinmemory.network.LlmApi
import io.github.gelassen.wordinmemory.network.LlmBaseUrlInterceptor
import io.github.gelassen.wordinmemory.repository.ChatRepository
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
        const val OKHTTP_CLIENT = "OKHTTP_CLIENT"
        const val LLM_OKHTTP_CLIENT = "LLM_OKHTTP_CLIENT"
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
    @Named(Consts.OKHTTP_CLIENT)
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
    fun provideApi(@Named(Consts.OKHTTP_CLIENT) httpClient: OkHttpClient): IApi {
        // Placeholder base URL; DynamicBaseUrlInterceptor rewrites host/port from prefs
        val url = application.getString(R.string.endpoint)
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
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

    /**
     * Same signature as before: listener is optional and provided as null at app scope.
     * Call sites that need callbacks pass their own listener into translate methods.
     */
    @Provides
    fun provideTranslator(): PlainTranslator {
        return PlainTranslator(null)
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

    // ── LLM chat backend (provider-agnostic) ───────────────────────────────

    @Provides
    @Singleton
    @Named(Consts.LLM_OKHTTP_CLIENT)
    fun provideLlmOkHttpClient(sharedPreferences: SharedPreferences): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(240, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(LlmBaseUrlInterceptor(sharedPreferences))
            .addInterceptor(logging)
            .build()
    }

    @Singleton
    @Provides
    fun provideLlmApi(
        @Named(Consts.LLM_OKHTTP_CLIENT) llmClient: OkHttpClient,
        sharedPreferences: SharedPreferences
    ): LlmApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(LlmBaseUrlInterceptor.resolveBaseUrl(sharedPreferences))
            .client(llmClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(LlmApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        llmApi: LlmApi,
        sharedPreferences: SharedPreferences
    ): ChatRepository {
        return ChatRepository(llmApi, sharedPreferences)
    }
}
