package com.aura.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.data.AppEntry
import com.aura.launcher.data.AppRepository
import com.aura.launcher.data.WatchEntry
import com.aura.launcher.data.WatchHistoryStore
import com.aura.launcher.service.MediaWatchService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val watchStore = WatchHistoryStore.get(application)
    val watch: StateFlow<List<WatchEntry>> = watchStore.entries

    private val _watchEnabled = MutableStateFlow(false)
    val watchEnabled: StateFlow<Boolean> = _watchEnabled.asStateFlow()

    fun forget(packageName: String) = watchStore.remove(packageName)

    init {
        refresh()
    }

    fun refresh() {
        _watchEnabled.value = MediaWatchService.isEnabled(getApplication())
        viewModelScope.launch {
            _apps.value = repository.loadApps()
            _loaded.value = true
        }
    }
}
