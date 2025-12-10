package com.example.dresscode.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dresscode.model.Gender
import com.example.dresscode.model.ModelPreferences
import com.example.dresscode.model.OutfitFilters
import com.example.dresscode.model.TaggingModel
import com.example.dresscode.model.TryOnModel
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
    private val tryOnModelKey = stringPreferencesKey("try_on_model")
    private val taggingModelKey = stringPreferencesKey("tagging_model")

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

    fun modelPreferences(): Flow<ModelPreferences> {
        return context.settingsDataStore.data.map { prefs ->
            ModelPreferences(
                tryOnModel = TryOnModel.fromValue(prefs[tryOnModelKey]),
                taggingModel = TaggingModel.fromValue(prefs[taggingModelKey])
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

    suspend fun setTryOnModel(model: TryOnModel) {
        context.settingsDataStore.edit { prefs ->
            prefs[tryOnModelKey] = model.value
        }
    }

    suspend fun setTaggingModel(model: TaggingModel) {
        context.settingsDataStore.edit { prefs ->
            prefs[taggingModelKey] = model.value
        }
    }
}
