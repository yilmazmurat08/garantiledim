package com.garantiledim.app.ui.profile

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garantiledim.app.AppContainer
import com.garantiledim.app.BuildConfig
import com.garantiledim.app.R
import com.garantiledim.app.data.InvalidBackupException
import com.garantiledim.app.data.UserSettings
import com.garantiledim.app.domain.CONTACT_EMAIL
import com.garantiledim.app.domain.DurationType
import com.garantiledim.app.domain.Photo
import com.garantiledim.app.i18n.LanguageChoice
import com.garantiledim.app.i18n.LocalStrings
import com.garantiledim.app.i18n.Strings
import com.garantiledim.app.notifications.Reminders
import com.garantiledim.app.ui.appViewModelFactory
import com.garantiledim.app.ui.components.Avatar
import com.garantiledim.app.ui.components.ConfirmDialog
import com.garantiledim.app.ui.components.GIcon
import com.garantiledim.app.ui.components.LabeledTextField
import com.garantiledim.app.ui.components.SectionLabel
import com.garantiledim.app.ui.components.TimePickerSheet
import com.garantiledim.app.ui.components.openNotificationSettings
import com.garantiledim.app.ui.components.rememberNotificationsEnabled
import com.garantiledim.app.ui.components.surfaceCard
import com.garantiledim.app.ui.legal.LegalDoc
import com.garantiledim.app.ui.legal.sendContactEmail
import com.garantiledim.app.ui.premium.PaywallReason
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GShapes
import com.garantiledim.app.ui.theme.GType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<UserSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val premium: StateFlow<Boolean> = container.premium.isPremium

    /** Yedekleme ya da geri yükleme sürüyor. */
    val busy = MutableStateFlow(false)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 2)

    val language: StateFlow<LanguageChoice> = container.language.choice

    /** Dil değişince bildirim kanalının adı da güncellenir; ekran yeniden kurulur. */
    fun setLanguage(choice: LanguageChoice) {
        container.language.set(choice)
        Reminders.createChannel(container.appContext, container.language.current)
    }

    private val t get() = container.language.current

    fun backup(target: Uri) = runBackupTask {
        t.backupSaved(container.backup.export(target))
    }

    fun restore(source: Uri) = runBackupTask {
        t.backupRestored(container.backup.restore(source))
    }

    private fun runBackupTask(block: suspend () -> String) {
        if (busy.value) return
        // Yedekleme Premium'a dahil; arayüzden bağımsız olarak burada da denetlenir.
        if (!premium.value) {
            messages.tryEmit(t.backupNeedsPremium)
            return
        }
        busy.value = true
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: InvalidBackupException) {
                when (e.reason) {
                    InvalidBackupException.Reason.CORRUPT -> t.backupCorrupt
                    InvalidBackupException.Reason.NOT_A_BACKUP -> t.backupNotOurs
                    InvalidBackupException.Reason.TOO_NEW -> t.backupTooNew
                }
            } catch (e: Exception) {
                t.operationFailed
            }
            busy.value = false
            messages.tryEmit(message)
        }
    }

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

private enum class ProfileDialog { NAME, REMINDER_DAYS, TIME, RESTORE, LANGUAGE }

/** Dil adları her dilde kendi adıyla gösterilir. */
private fun LanguageChoice.label(s: Strings): String = when (this) {
    LanguageChoice.SYSTEM -> s.languageSystem
    LanguageChoice.TR -> "Türkçe"
    LanguageChoice.EN -> "English"
}

private val reminderOptions = listOf(1, 2, 3, 5, 7)

@Composable
fun ProfileScreen(
    onOpenLegal: (LegalDoc) -> Unit,
    onOpenPremium: (PaywallReason) -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = appViewModelFactory { c, _ -> ProfileViewModel(c) }),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val str = LocalStrings.current
    val activity = LocalActivity.current
    val premium by viewModel.premium.collectAsStateWithLifecycle()
    val languageChoice by viewModel.language.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    val backupTarget = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::backup)
    }
    val restoreSource = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restore)
    }
    val s = settings ?: return
    val notificationsEnabled = rememberNotificationsEnabled()
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<ProfileDialog?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::setPhoto)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(str.tabProfile, style = GType.ScreenTitle, color = GColors.Text)

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
                    modifier = Modifier.clickable(onClickLabel = str.choosePhotoA11y) {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                Column(Modifier.weight(1f)) {
                    Text(s.name.ifBlank { str.addYourName }, style = GType.SectionTitle, color = GColors.Text)
                    Text(str.storedOnDevice, style = GType.Caption, color = GColors.TextSecondary)
                }
                IconButton(onClick = { dialog = ProfileDialog.NAME }) {
                    GIcon(R.drawable.ic_edit, tint = GColors.Text, size = 20.dp, contentDescription = str.editNameA11y)
                }
            }
            if (s.photo != null) {
                TextButton(onClick = viewModel::removePhoto, modifier = Modifier.padding(top = 0.dp)) {
                    Text(str.removeProfilePhoto, style = GType.Label, color = GColors.TextSecondary)
                }
            }

            SettingsGroup(str.premium) {
                if (premium) {
                    SettingsRow(title = str.premiumTitle, value = str.premiumActiveSummary, badge = str.active)
                } else {
                    SettingsRow(
                        title = str.goPremium,
                        value = str.goPremiumSummary,
                        valueColor = GColors.Pink,
                        onClick = { onOpenPremium(PaywallReason.GENERAL) },
                    )
                }
            }

            SettingsGroup(str.backupSection) {
                SettingsRow(
                    title = str.backupAction,
                    value = if (busy) str.processing else str.backupSummary,
                    badge = if (premium) null else "Premium",
                    onClick = {
                        when {
                            busy -> Unit
                            !premium -> onOpenPremium(PaywallReason.BACKUP)
                            else -> try {
                                backupTarget.launch("${str.backupFilePrefix}-${LocalDate.now()}.zip")
                            } catch (e: ActivityNotFoundException) {
                                viewModel.messages.tryEmit(str.saveScreenMissing)
                            }
                        }
                    },
                )
                HorizontalDivider(color = GColors.SurfaceHigh)
                SettingsRow(
                    title = str.restoreAction,
                    value = str.restoreSummary,
                    badge = if (premium) null else "Premium",
                    onClick = {
                        when {
                            busy -> Unit
                            !premium -> onOpenPremium(PaywallReason.BACKUP)
                            else -> dialog = ProfileDialog.RESTORE
                        }
                    },
                )
            }

            SettingsGroup(str.languageSection) {
                SettingsRow(
                    title = str.appLanguage,
                    value = languageChoice.label(str),
                    onClick = { dialog = ProfileDialog.LANGUAGE },
                )
            }

            SettingsGroup(str.remindersSection) {
                SettingsRow(
                    title = str.reminderStart,
                    value = str.daysBeforeEnd(s.reminderLeadDays),
                    onClick = { dialog = ProfileDialog.REMINDER_DAYS },
                )
                HorizontalDivider(color = GColors.SurfaceHigh)
                SettingsRow(
                    title = str.notificationTime,
                    value = str.everyDayAt(str.time(s.notifyHour, s.notifyMinute)),
                    onClick = { dialog = ProfileDialog.TIME },
                )
                HorizontalDivider(color = GColors.SurfaceHigh)
                SettingsRow(
                    title = str.notificationPermission,
                    value = if (notificationsEnabled) str.permissionOn else str.permissionOffTap,
                    valueColor = if (notificationsEnabled) GColors.Success else GColors.Danger,
                    onClick = { openNotificationSettings(context) },
                )
            }

            SettingsGroup(str.defaultPeriods) {
                SettingsRow(title = str.duration(DurationType.IADE), trailing = str.days14)
                HorizontalDivider(color = GColors.SurfaceHigh)
                SettingsRow(title = str.duration(DurationType.GARANTI), trailing = str.years2)
            }

            SettingsGroup(str.legalSection) {
                SettingsRow(title = LegalDoc.PRIVACY.title(str), onClick = { onOpenLegal(LegalDoc.PRIVACY) })
                HorizontalDivider(color = GColors.SurfaceHigh)
                SettingsRow(title = LegalDoc.TERMS.title(str), onClick = { onOpenLegal(LegalDoc.TERMS) })
            }

            SettingsGroup(str.otherSection) {
                SettingsRow(
                    title = str.contactUs,
                    value = CONTACT_EMAIL,
                    onClick = { sendContactEmail(context, "Garantiledim") },
                )
                HorizontalDivider(color = GColors.SurfaceHigh)
                SettingsRow(title = str.about, trailing = str.version(BuildConfig.VERSION_NAME))
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
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
        ProfileDialog.RESTORE -> ConfirmDialog(
            title = str.restoreConfirmTitle,
            text = str.restoreConfirmBody,
            confirmLabel = str.chooseBackup,
            onConfirm = {
                try {
                    restoreSource.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                } catch (e: ActivityNotFoundException) {
                    viewModel.messages.tryEmit(str.filePickerMissing)
                }
            },
            onDismiss = { dialog = null },
        )
        ProfileDialog.LANGUAGE -> ChoiceDialog(
            title = str.appLanguage,
            options = LanguageChoice.entries,
            selected = languageChoice,
            label = { it.label(str) },
            onDismiss = { dialog = null },
            onSelect = { choice ->
                if (choice != languageChoice) {
                    viewModel.setLanguage(choice)
                    // Tarih seçici gibi sistem bileşenleri de yeni dilde açılsın diye ekran yeniden kurulur.
                    activity?.recreate()
                }
            },
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
    valueColor: Color = GColors.TextSecondary,
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
            if (value != null) Text(value, style = GType.Caption, color = valueColor)
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
    val s = LocalStrings.current
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GColors.Surface,
        title = { Text(s.yourName, style = GType.SectionTitle, color = GColors.Text) },
        text = { LabeledTextField(label = s.nameHint, value = name, onValueChange = { name = it }) },
        confirmButton = {
            TextButton(onClick = {
                onSave(name)
                onDismiss()
            }) { Text(s.save, style = GType.Label, color = GColors.Pink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel, style = GType.Label, color = GColors.TextSecondary) }
        },
    )
}

@Composable
private fun ReminderDaysDialog(selected: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) =
    ChoiceDialog(
        title = LocalStrings.current.reminderStart,
        options = reminderOptions,
        selected = selected,
        label = { LocalStrings.current.daysBeforeEnd(it) },
        onDismiss = onDismiss,
        onSelect = onSelect,
    )

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GColors.Surface,
        title = { Text(title, style = GType.SectionTitle, color = GColors.Text) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = GColors.Pink, unselectedColor = GColors.TextMuted),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(label(option), style = GType.Body, color = GColors.Text)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.close, style = GType.Label, color = GColors.TextSecondary) }
        },
    )
}
