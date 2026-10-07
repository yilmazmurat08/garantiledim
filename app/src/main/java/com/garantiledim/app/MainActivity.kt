package com.garantiledim.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.garantiledim.app.notifications.EXTRA_PRODUCT_ID
import com.garantiledim.app.ui.AppRoot
import com.garantiledim.app.ui.theme.GarantiledimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {

    /** Bildirimden açılan ürün; AppRoot detay ekranına gider ve değeri sıfırlar. */
    private val openProduct = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) handleIntent(intent)

        val clock = (application as GarantiledimApp).container.today
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    clock.refresh()
                    delay(millisUntilMidnight())
                }
            }
        }

        setContent {
            GarantiledimTheme {
                AppRoot(openProduct = openProduct)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_PRODUCT_ID, -1L) ?: -1L
        if (id > 0) openProduct.value = id
    }

    private fun millisUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = LocalDate.now().plusDays(1).atStartOfDay()
        return Duration.between(now, midnight).toMillis().coerceAtLeast(1_000) + 1_000
    }
}
