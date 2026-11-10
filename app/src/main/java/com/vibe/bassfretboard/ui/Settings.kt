package com.vibe.bassfretboard.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val tuningId: String = "standard4",
    val fretCount: Int = 15,
    val vertical: Boolean = false,
    val leftHanded: Boolean = false,
    val showNoteNames: Boolean = true,
)

class SettingsRepository(context: Context) {
    private val dataStore = context.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            tuningId = prefs[Keys.TUNING_ID] ?: "standard4",
            fretCount = prefs[Keys.FRET_COUNT] ?: 15,
            vertical = prefs[Keys.VERTICAL] ?: false,
            leftHanded = prefs[Keys.LEFT_HANDED] ?: false,
            showNoteNames = prefs[Keys.SHOW_NOTE_NAMES] ?: true,
        )
    }

    suspend fun setTuningId(id: String) = dataStore.edit { it[Keys.TUNING_ID] = id }

    suspend fun setFretCount(count: Int) = dataStore.edit { it[Keys.FRET_COUNT] = count }

    suspend fun setVertical(vertical: Boolean) = dataStore.edit { it[Keys.VERTICAL] = vertical }

    suspend fun setLeftHanded(leftHanded: Boolean) = dataStore.edit { it[Keys.LEFT_HANDED] = leftHanded }

    suspend fun setShowNoteNames(show: Boolean) = dataStore.edit { it[Keys.SHOW_NOTE_NAMES] = show }

    private object Keys {
        val TUNING_ID = stringPreferencesKey("tuning_id")
        val FRET_COUNT = intPreferencesKey("fret_count")
        val VERTICAL = booleanPreferencesKey("vertical")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val SHOW_NOTE_NAMES = booleanPreferencesKey("show_note_names")
    }
}
