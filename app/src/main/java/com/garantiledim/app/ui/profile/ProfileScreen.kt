package com.garantiledim.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.BuildConfig
import com.garantiledim.app.R
import com.garantiledim.app.data.UserSettings
import com.garantiledim.app.domain.Photo
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.Avatar
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.LabeledTextField
import com.garantiledim.app.ui.components.SectionLabel
import com.garantiledim.app.ui.components.TimePickerSheet
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<UserSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setName(name: String) {
        viewModelScope.launch { container.settings.setName(name) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val photo = runCatching { container.files.importPhoto(uri) }.getOrNull() ?: return@launch
            replacePhoto(photo)
        }
    }

    fun removePhoto() {
        viewModelScope.launch { replacePhoto(null) }
    }

    private suspend fun replacePhoto(photo: Photo?) {
        val old = settings.value?.photo
        container.settings.setPhoto(photo)
        if (old != null) container.files.delete(listOfNotNull(old.path, old.thumbPath))
    }

    fun setReminderDays(days: Int) {
        viewModelScope.launch { container.settings.setReminderLeadDays(days) }
    }

    fun setNotifyTime(hour: Int, minute: Int) {
        viewModelScope.launch { container.settings.setNotifyTime(hour, minute) }
    }
}

private enum class ProfileDialog { NAME, REMINDER_DAYS, TIME }

private val reminderOptions = listOf(1, 2, 3, 5, 7)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(factory = appViewModelFactory { c, _ -> ProfileViewModel(c) }),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::setPhoto)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Profil & Ayarlar", style = GType.ScreenTitle, color = GColors.Text)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .surfaceCard(GShapes.Header)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatar(
                name = s.name,
                photoPath = s.photo?.thumbPath ?: s.photo?.path,
                size = 68.dp,
                modifier = Modifier.clickable(onClickLabel = "Fotoğraf seç") {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            Column(Modifier.weight(1f)) {
                Text(s.name.ifBlank { "Adını ekle" }, style = GType.SectionTitle, color = GColors.Text)
                Text("Bilgilerin sadece bu cihazda saklanır", style = GType.Caption, color = GColors.TextSecondary)
            }
            IconButton(onClick = { dialog = ProfileDialog.NAME }) {
                GIcon(R.drawable.ic_edit, tint = GColors.Text, size = 20.dp, contentDescription = "Adı düzenle")
            }
        }
        if (s.photo != null) {
            TextButton(onClick = viewModel::removePhoto, modifier = Modifier.padding(top = 0.dp)) {
                Text("Profil fotoğrafını kaldır", style = GType.Label, color = GColors.TextSecondary)
            }
        }

        SettingsGroup("Hatırlatmalar") {
            SettingsRow(
                title = "Hatırlatma başlangıcı",
                value = "Bitişten ${s.reminderLeadDays} gün önce",
                onClick = { dialog = ProfileDialog.REMINDER_DAYS },
            )
            HorizontalDivider(color = GColors.SurfaceHigh)
            SettingsRow(
                title = "Bildirim saati",
                value = "Her gün " + String.format(Locale.ROOT, "%02d:%02d", s.notifyHour, s.notifyMinute),
                onClick = { dialog = ProfileDialog.TIME },
            )
        }

        SettingsGroup("Varsayılan süreler") {
            SettingsRow(title = "İade hakkı", trailing = "14 gün")
            HorizontalDivider(color = GColors.SurfaceHigh)
            SettingsRow(title = "Garanti", trailing = "2 yıl")
        }

        SettingsGroup("Diğer") {
            SettingsRow(title = "Yedekleme ve Premium", badge = "Yakında")
            HorizontalDivider(color = GColors.SurfaceHigh)
            SettingsRow(title = "Hakkında", trailing = "Sürüm ${BuildConfig.VERSION_NAME}")
        }
    }

    when (dialog) {
        ProfileDialog.NAME -> NameDialog(
            initial = s.name,
            onDismiss = { dialog = null },
            onSave = viewModel::setName,
        )
        ProfileDialog.REMINDER_DAYS -> ReminderDaysDialog(
            selected = s.reminderLeadDays,
            onDismiss = { dialog = null },
            onSelect = viewModel::setReminderDays,
        )
        ProfileDialog.TIME -> TimePickerSheet(
            hour = s.notifyHour,
            minute = s.notifyMinute,
            onDismiss = { dialog = null },
            onPick = viewModel::setNotifyTime,
        )
        null -> Unit
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        Column(Modifier.fillMaxWidth().surfaceCard()) { content() }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    trailing: String? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = GType.BodyMedium, color = GColors.Text)
            if (value != null) Text(value, style = GType.Caption, color = GColors.TextSecondary)
        }
        if (trailing != null) Text(trailing, style = GType.Caption.copy(fontSize = GType.Caption.fontSize * 1.08f), color = GColors.TextSecondary)
        if (badge != null) {
            Text(
                badge,
                style = GType.Badge,
                color = GColors.TextSecondary,
                modifier = Modifier
                    .background(GColors.SurfaceHigh, GShapes.Pill)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        if (onClick != null) GIcon(R.drawable.ic_chevron_right, tint = GColors.Lavender, size = 18.dp)
    }
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GColors.Surface,
        title = { Text("Adın", style = GType.SectionTitle, color = GColors.Text) },
        text = { LabeledTextField(label = "Karşılama ekranında görünür", value = name, onValueChange = { name = it }) },
        confirmButton = {
            TextButton(onClick = {
                onSave(name)
                onDismiss()
            }) { Text("Kaydet", style = GType.Label, color = GColors.Pink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", style = GType.Label, color = GColors.TextSecondary) }
        },
    )
}

@Composable
private fun ReminderDaysDialog(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GColors.Surface,
        title = { Text("Hatırlatma başlangıcı", style = GType.SectionTitle, color = GColors.Text) },
        text = {
            Column {
                reminderOptions.forEach { days ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = days == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(days)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = days == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = GColors.Pink, unselectedColor = GColors.TextMuted),
                            modifier = Modifier.size(48.dp),
                        )
                        Text("Bitişten $days gün önce", style = GType.Body, color = GColors.Text)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat", style = GType.Label, color = GColors.TextSecondary) }
        },
    )
}
