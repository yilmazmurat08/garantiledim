package com.garantiledim.app

import android.app.Application
import android.content.Context
import com.garantiledim.app.data.AppDatabase
import com.garantiledim.app.data.FileStore
import com.garantiledim.app.data.ProductRepository
import com.garantiledim.app.data.SettingsRepository
import com.garantiledim.app.notifications.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class GarantiledimApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
        // Günlük kontrol açılışta ve bildirim saati her değiştiğinde yeniden zamanlanır.
        container.appScope.launch {
            container.settings.settings
                .map { it.notifyHour to it.notifyMinute }
                .distinctUntilChanged()
                .collect { (hour, minute) -> Reminders.schedule(this@GarantiledimApp, hour, minute) }
        }
    }
}

/** Uygulama genelinde paylaşılan nesneler. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = AppDatabase.build(context)
    val files = FileStore(context)
    val products = ProductRepository(database.productDao(), files)
    val settings = SettingsRepository(context)
    val today = TodayClock()
}

/** Bugünün tarihi; uygulama ön plana geldiğinde ve gece yarısı yenilenir. */
class TodayClock {
    private val state = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = state.asStateFlow()

    fun refresh() {
        state.value = LocalDate.now()
    }
}
