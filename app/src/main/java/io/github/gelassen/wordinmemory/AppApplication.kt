package io.github.gelassen.wordinmemory

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.work.Configuration
import io.github.gelassen.wordinmemory.backgroundjobs.MyWorkerFactory
import io.github.gelassen.wordinmemory.di.AppComponent
import io.github.gelassen.wordinmemory.di.AppModule
import io.github.gelassen.wordinmemory.di.DaggerAppComponent
import javax.inject.Inject


class AppApplication: Application(), Configuration.Provider {

    protected lateinit var diComponent: AppComponent

    @Inject
    lateinit var myWorkerFactory: MyWorkerFactory

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            turnOnNetworkStrictPolicy()
        }

        diComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(this))
            .build()
        diComponent.inject(this)
    }

    fun getComponent(): AppComponent {
        return diComponent
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(myWorkerFactory)
            .build()
    }

    private fun turnOnNetworkStrictPolicy() {
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() // or .detectAll() for all detectable problems
                .penaltyLog()
//                .penaltyDeath()
                .build()
        )
    }

}