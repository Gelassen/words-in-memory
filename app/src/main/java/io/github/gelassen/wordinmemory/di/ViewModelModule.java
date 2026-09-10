package io.github.gelassen.wordinmemory.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import io.github.gelassen.wordinmemory.ui.addnewrecord.NewRecordViewModel;
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardViewModel;
import io.github.gelassen.wordinmemory.ui.chat.ChatViewModel;

@Module
public abstract class ViewModelModule {

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory viewModelFactory);

    @Binds
    @IntoMap
    @ViewModelKey(DashboardViewModel.class)
    @Singleton
    abstract ViewModel dashboardViewModel(DashboardViewModel vm);

    @Binds
    @IntoMap
    @ViewModelKey(NewRecordViewModel.class)
    @Singleton
    abstract ViewModel newRecordViewModel(NewRecordViewModel vm);

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel.class)
    @Singleton
    abstract ViewModel chatViewModel(ChatViewModel vm);
}
