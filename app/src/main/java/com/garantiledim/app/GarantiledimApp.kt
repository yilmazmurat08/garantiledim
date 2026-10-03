package com.garantiledim.app

import android.app.Application
import android.content.Context
import com.garantiledim.app.data.AppDatabase
import com.garantiledim.app.data.FileStore
import com.garantiledim.app.data.ProductRepository
import com.garantiledim.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class GarantiledimApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Uygulama genelinde paylaşılan nesneler. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val files = FileStore(context)
    val products = ProductRepository(AppDatabase.build(context).productDao(), files)
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
