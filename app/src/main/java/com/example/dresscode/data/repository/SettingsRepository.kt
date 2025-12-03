package com.example.dresscode.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dresscode.model.Gender
import com.example.dresscode.model.OutfitFilters
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val genderKey = stringPreferencesKey("default_gender")
    private val defaultTagKey = stringPreferencesKey("default_tag")

    fun defaultFilters(): Flow<OutfitFilters> {
        return context.settingsDataStore.data.map { prefs ->
            val gender = prefs[genderKey]?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
            val defaultTag = prefs[defaultTagKey]
            OutfitFilters(
                gender = gender,
                tags = defaultTag?.let { setOf(it) } ?: emptySet()
            )
        }
    }

    suspend fun setDefaultGender(gender: Gender?) {
        context.settingsDataStore.edit { prefs ->
            if (gender == null) {
                prefs.remove(genderKey)
            } else {
                prefs[genderKey] = gender.name
            }
        }
    }

    suspend fun setDefaultTag(tag: String?) {
        context.settingsDataStore.edit { prefs ->
            if (tag.isNullOrBlank()) {
                prefs.remove(defaultTagKey)
            } else {
                prefs[defaultTagKey] = tag
            }
        }
    }
}
