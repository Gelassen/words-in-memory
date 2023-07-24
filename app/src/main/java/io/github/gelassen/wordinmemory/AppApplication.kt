package io.github.gelassen.wordinmemory

import android.app.Application
import io.github.gelassen.wordinmemory.di.AppComponent
import io.github.gelassen.wordinmemory.di.AppModule
import io.github.gelassen.wordinmemory.di.DaggerAppComponent

class AppApplication: Application() {

    protected lateinit var diComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        diComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(this))
            .build()
    }

    fun getComponent(): AppComponent {
        return diComponent
    }

}