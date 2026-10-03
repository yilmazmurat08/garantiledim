package com.garantiledim.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import com.garantiledim.app.ui.theme.GColors
import com.garantiledim.app.ui.theme.GType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Tarih seçici. [minDate] verilirse ondan önceki günler seçilemez. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minDate == null || !utcTimeMillis.toUtcDate().isBefore(minDate)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(it.toUtcDate()) }
                onDismiss()
            }) { Text("Tamam", style = GType.Label, color = GColors.Pink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", style = GType.Label, color = GColors.TextSecondary) }
        },
        colors = DatePickerDefaults.colors(containerColor = GColors.Surface),
    ) {
        DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = GColors.Surface))
    }
}

/** 24 saat biçiminde saat seçici. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onPick: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GColors.Surface,
        title = { Text("Bildirim saati", style = GType.SectionTitle, color = GColors.Text) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = GColors.SurfaceHigh,
                    timeSelectorSelectedContainerColor = GColors.Pink,
                    timeSelectorSelectedContentColor = GColors.OnPastel,
                    timeSelectorUnselectedContainerColor = GColors.SurfaceHigh,
                    timeSelectorUnselectedContentColor = GColors.Text,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(state.hour, state.minute)
                onDismiss()
            }) { Text("Tamam", style = GType.Label, color = GColors.Pink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", style = GType.Label, color = GColors.TextSecondary) }
        },
    )
}

/** Onay diyaloğu (silme gibi geri alınamayan işlemler için). */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GColors.Surface,
        title = { Text(title, style = GType.SectionTitle, color = GColors.Text) },
        text = { Text(text, style = GType.Body, color = GColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmLabel, style = GType.Label, color = GColors.Danger) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç", style = GType.Label, color = GColors.TextSecondary) }
        },
    )
}
