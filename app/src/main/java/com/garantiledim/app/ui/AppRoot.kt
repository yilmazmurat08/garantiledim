package com.garantiledim.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.garantiledim.app.AppContainer
import com.garantiledim.app.GarantiledimApp
import com.garantiledim.app.R
import com.garantiledim.app.domain.canAddProduct
import com.garantiledim.app.ui.agenda.AgendaScreen
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.detail.ProductDetailScreen
import com.garantiledim.app.ui.edit.EditProductScreen
import com.garantiledim.app.ui.home.HomeScreen
import com.garantiledim.app.ui.legal.LegalDoc
import com.garantiledim.app.ui.legal.LegalScreen
import com.garantiledim.app.ui.list.ProductListScreen
import com.garantiledim.app.ui.premium.PaywallReason
import com.garantiledim.app.ui.premium.PaywallScreen
import com.garantiledim.app.ui.profile.ProfileScreen
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

enum class Tab(val route: String, val label: String, @DrawableRes val icon: Int) {
    HOME("home", "Ana Sayfa", R.drawable.ic_home),
    LIST("list", "Garanti Belgelerim", R.drawable.ic_document),
    AGENDA("agenda", "Ajanda ve Hatırlatıcılar", R.drawable.ic_calendar_check),
    PROFILE("profile", "Profil & Ayarlar", R.drawable.ic_settings),
}

private object Routes {
    const val EDIT = "edit?id={id}"
    const val DETAIL = "detail/{id}"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
    fun detail(id: Long) = "detail/$id"
    const val LEGAL = "legal/{doc}"
    fun legal(doc: LegalDoc) = "legal/${doc.route}"
    const val PREMIUM = "premium?reason={reason}"
    fun premium(reason: PaywallReason = PaywallReason.GENERAL) = "premium?reason=${reason.route}"
}

const val NEW_PRODUCT_ID = -1L

/** ViewModel'leri uygulama nesneleriyle oluşturan fabrika. */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GarantiledimApp
        create(app.container, createSavedStateHandle())
    }
}

@Composable
fun AppRoot(openProduct: MutableStateFlow<Long?>) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { it.route == backStack?.destination?.route }
    val context = LocalContext.current
    val container = remember { (context.applicationContext as GarantiledimApp).container }
    val productCount by remember { container.products.products.map { it.size } }.collectAsStateWithLifecycle(0)
    val premium by container.premium.isPremium.collectAsStateWithLifecycle()

    /** Ücretsiz sınır dolduysa ürün ekleme yerine Premium ekranı açılır. */
    fun openAdd(entry: NavBackStackEntry) = entry.ifResumed {
        if (canAddProduct(productCount, premium)) {
            navController.navigate(Routes.edit())
        } else {
            navController.navigate(Routes.premium(PaywallReason.LIMIT))
        }
    }

    // Bildirime dokunulunca ilgili ürünün detayı açılır.
    val pendingProduct by openProduct.collectAsStateWithLifecycle()
    LaunchedEffect(pendingProduct) {
        val id = pendingProduct ?: return@LaunchedEffect
        navController.navigate(Routes.detail(id)) { launchSingleTop = true }
        openProduct.value = null
    }

    // Bildirim izni ilk ürün kaydedildiğinde istenir (Android 13+).
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val askNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = GColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                BottomBar(current = currentTab, onSelect = { navController.openTab(it) })
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.HOME.route) { entry ->
                HomeScreen(
                    onAddProduct = { openAdd(entry) },
                    onOpenProduct = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
                    onOpenTab = { navController.openTab(it) },
                    onOpenLegal = { doc -> entry.ifResumed { navController.navigate(Routes.legal(doc)) } },
                )
            }
            composable(Tab.LIST.route) { entry ->
                ProductListScreen(
                    onAddProduct = { openAdd(entry) },
                    onOpenProduct = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
                )
            }
            composable(Tab.AGENDA.route) { entry ->
                AgendaScreen(
                    onOpenProduct = { id -> entry.ifResumed { navController.navigate(Routes.detail(id)) } },
                    onOpenSettings = { navController.openTab(Tab.PROFILE) },
                )
            }
            composable(Tab.PROFILE.route) { entry ->
                ProfileScreen(
                    onOpenLegal = { doc -> entry.ifResumed { navController.navigate(Routes.legal(doc)) } },
                    onOpenPremium = { reason -> entry.ifResumed { navController.navigate(Routes.premium(reason)) } },
                )
            }
            composable(
                route = Routes.LEGAL,
                arguments = listOf(navArgument("doc") { type = NavType.StringType }),
            ) { entry ->
                val doc = LegalDoc.entries.firstOrNull { it.route == entry.arguments?.getString("doc") }
                    ?: LegalDoc.PRIVACY
                LegalScreen(doc = doc, onBack = { entry.ifResumed { navController.popBackStack() } })
            }
            composable(
                route = Routes.PREMIUM,
                arguments = listOf(navArgument("reason") {
                    type = NavType.StringType
                    defaultValue = PaywallReason.GENERAL.route
                }),
            ) { entry ->
                val reason = PaywallReason.entries.firstOrNull { it.route == entry.arguments?.getString("reason") }
                    ?: PaywallReason.GENERAL
                PaywallScreen(
                    reason = reason,
                    onClose = { entry.ifResumed { navController.popBackStack() } },
                    onOpenLegal = { doc -> entry.ifResumed { navController.navigate(Routes.legal(doc)) } },
                )
            }
            composable(
                route = Routes.EDIT,
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = NEW_PRODUCT_ID
                }),
            ) { entry ->
                EditProductScreen(
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                    onCreated = { id ->
                        askNotificationPermission()
                        navController.navigate(Routes.detail(id)) {
                            popUpTo(Routes.EDIT) { inclusive = true }
                        }
                    },
                    onSaved = { navController.popBackStack() },
                    onDeleted = {
                        if (!navController.popBackStack(Routes.DETAIL, inclusive = true)) {
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                ProductDetailScreen(
                    onOpenPremium = { entry.ifResumed { navController.navigate(Routes.premium(PaywallReason.LIMIT)) } },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                    onEdit = { id -> entry.ifResumed { navController.navigate(Routes.edit(id)) } },
                )
            }
        }
    }
}

/**
 * Ekran geçişi sürerken gelen ikinci dokunuşu yok sayar. Aksi halde geri tuşuna hızlı iki kez
 * basmak ana ekranı da yığından çıkarıp boş ekran bırakır, karta iki kez dokunmak detayı iki kez açar.
 */
private inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Yüzen alt menü (SPEC.md madde 4.0). */
@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
            .fillMaxWidth()
            .clip(GShapes.Nav)
            .background(GColors.Surface)
            .border(1.dp, GColors.Outline, GShapes.Nav)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val color = if (selected) GColors.Pink else GColors.Lavender
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .selectable(selected = selected, onClick = { onSelect(tab) }, role = Role.Tab)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(if (selected) GColors.Pink else Color.Transparent, RoundedCornerShape(2.dp))
                )
                Box(Modifier.padding(top = 2.dp)) { GIcon(tab.icon, tint = color) }
                Text(
                    text = tab.label,
                    style = GType.NavLabel,
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}
