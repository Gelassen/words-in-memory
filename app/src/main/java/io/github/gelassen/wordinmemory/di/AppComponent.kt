package io.github.gelassen.wordinmemory.di

import dagger.Component
import io.github.gelassen.wordinmemory.ui.dashboard.AddItemBottomSheetDialogFragment
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ViewModelModule::class,
        AppModule::class
    ]
)
interface AppComponent {
    fun inject(subj: AddItemBottomSheetDialogFragment)
    fun inject(subj: DashboardFragment)
}