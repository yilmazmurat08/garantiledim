package com.garantiledim.app.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.R
import com.garantiledim.app.data.UserSettings
import com.garantiledim.app.domain.AgendaEntry
import com.garantiledim.app.domain.AgendaGroup
import com.garantiledim.app.domain.TrDates
import com.garantiledim.app.domain.agendaGroups
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.EmptyMessage
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.SectionLabel
import com.garantiledim.app.ui.components.StatusBadge
import com.garantiledim.app.ui.components.color
import com.garantiledim.app.ui.components.openNotificationSettings
import com.garantiledim.app.ui.components.rememberNotificationsEnabled
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

data class AgendaUiState(
    val loaded: Boolean = false,
    val groups: List<AgendaGroup> = emptyList(),
    val reminderText: String = "",
)

fun UserSettings.reminderSummary(): String =
    "Bitişten $reminderLeadDays gün önce başlar, her gün " +
        String.format(Locale.ROOT, "%02d:%02d", notifyHour, notifyMinute) + "'da"

class AgendaViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<AgendaUiState> = combine(
        container.products.products,
        container.settings.settings,
        container.today.today,
    ) { products, settings, today ->
        AgendaUiState(
            loaded = true,
            groups = agendaGroups(products, today),
            reminderText = settings.reminderSummary(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())
}

@Composable
fun AgendaScreen(
    onOpenProduct: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AgendaViewModel = viewModel(factory = appViewModelFactory { c, _ -> AgendaViewModel(c) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notificationsEnabled = rememberNotificationsEnabled()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Text("Ajanda ve Hatırlatıcılar", style = GType.ScreenTitle, color = GColors.Text)
        }
        if (!notificationsEnabled) {
            item(key = "notifications-off") {
                NotificationsOffCard(onOpen = { openNotificationSettings(context) }, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item(key = "reminder") {
            ReminderCard(state.reminderText, onOpenSettings, Modifier.padding(top = 6.dp, bottom = 6.dp))
        }
        if (state.loaded && state.groups.isEmpty()) {
            item(key = "empty") {
                EmptyMessage(
                    title = "Yaklaşan bir bitiş tarihi yok",
                    body = "Süresi devam eden ürünlerin bitiş tarihleri burada sıralanır.",
                )
            }
        }
        state.groups.forEach { group ->
            item(key = "group-${group.title}") {
                SectionLabel(group.title, Modifier.padding(top = 6.dp))
            }
            items(group.entries, key = { "${it.productId}-${it.deadline.type}" }) { entry ->
                AgendaRow(entry, onClick = { onOpenProduct(entry.productId) })
            }
        }
    }
}

@Composable
private fun NotificationsOffCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(GShapes.Card)
            .background(GColors.Danger.copy(alpha = 0.14f))
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GIcon(R.drawable.ic_bell, tint = GColors.Danger, size = 22.dp)
        Text(
            "Bildirimler kapalı, hatırlatma alamazsın",
            style = GType.Label,
            color = GColors.Text,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpen) {
            Text("Ayarlara git", style = GType.Label.copy(fontWeight = GType.Badge.fontWeight), color = GColors.Pink)
        }
    }
}

@Composable
private fun ReminderCard(text: String, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GShapes.Header)
            .background(GColors.PastelLavender)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(GShapes.Field)
                    .background(GColors.PastelLavenderInner),
                contentAlignment = Alignment.Center,
            ) {
                GIcon(R.drawable.ic_bell, tint = GColors.PastelLavenderIcon, size = 22.dp)
            }
            Column(Modifier.weight(1f)) {
                Text("Hatırlatma ayarı", style = GType.ActionTitle, color = GColors.OnPastel)
                Text(text, style = GType.Caption.copy(lineHeight = GType.Caption.fontSize * 1.4f), color = GColors.PastelLavenderText)
            }
        }
        Button(
            onClick = onOpenSettings,
            shape = GShapes.Inner,
            colors = ButtonDefaults.buttonColors(containerColor = GColors.OnPastel, contentColor = GColors.Text),
            modifier = Modifier.height(44.dp),
        ) {
            Text("Ayarla", style = GType.Label.copy(fontWeight = GType.Badge.fontWeight))
        }
    }
}

@Composable
private fun AgendaRow(entry: AgendaEntry, onClick: () -> Unit) {
    val color = entry.urgency.color()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .surfaceCard(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .height(52.dp)
                .clip(GShapes.Inner)
                .background(color.copy(alpha = 0.15f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("${entry.deadline.endDate.dayOfMonth}", style = GType.SectionTitle.copy(fontWeight = GType.ActionTitle.fontWeight), color = color)
            Text(TrDates.monthShortUpper(entry.deadline.endDate), style = GType.Badge.copy(fontSize = GType.Badge.fontSize * 0.9f, lineHeight = GType.Badge.fontSize), color = color)
        }
        Column(Modifier.weight(1f)) {
            Text(entry.productName, style = GType.BodyMedium.copy(fontWeight = GType.CardTitle.fontWeight), color = GColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.subtitle, style = GType.Caption, color = GColors.TextSecondary)
        }
        StatusBadge(entry.shortLabel, entry.urgency)
    }
}
