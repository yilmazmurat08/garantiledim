package com.garantiledim.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.garantiledim.app.domain.Photo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class UserSettings(
    val name: String = "",
    val photo: Photo? = null,
    val reminderLeadDays: Int = DEFAULT_REMINDER_DAYS,
    val notifyHour: Int = 10,
    val notifyMinute: Int = 0,
) {
    companion object {
        const val DEFAULT_REMINDER_DAYS = 3
    }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    private object Keys {
        val name = stringPreferencesKey("name")
        val photoPath = stringPreferencesKey("photo_path")
        val photoThumbPath = stringPreferencesKey("photo_thumb_path")
        val reminderDays = intPreferencesKey("reminder_days")
        val notifyHour = intPreferencesKey("notify_hour")
        val notifyMinute = intPreferencesKey("notify_minute")
    }

    val settings: Flow<UserSettings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            UserSettings(
                name = p[Keys.name].orEmpty(),
                photo = p[Keys.photoPath]?.let { Photo(it, p[Keys.photoThumbPath]) },
                reminderLeadDays = p[Keys.reminderDays] ?: UserSettings.DEFAULT_REMINDER_DAYS,
                notifyHour = p[Keys.notifyHour] ?: 10,
                notifyMinute = p[Keys.notifyMinute] ?: 0,
            )
        }

    suspend fun setName(name: String) {
        store.edit { it[Keys.name] = name.trim() }
    }

    suspend fun setPhoto(photo: Photo?) {
        store.edit {
            if (photo == null) {
                it.remove(Keys.photoPath)
                it.remove(Keys.photoThumbPath)
            } else {
                it[Keys.photoPath] = photo.path
                if (photo.thumbPath != null) it[Keys.photoThumbPath] = photo.thumbPath else it.remove(Keys.photoThumbPath)
            }
        }
    }

    suspend fun setReminderLeadDays(days: Int) {
        store.edit { it[Keys.reminderDays] = days }
    }

    suspend fun setNotifyTime(hour: Int, minute: Int) {
        store.edit {
            it[Keys.notifyHour] = hour
            it[Keys.notifyMinute] = minute
        }
    }
}
