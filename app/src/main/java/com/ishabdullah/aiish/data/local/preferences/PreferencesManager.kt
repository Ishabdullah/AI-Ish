/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aiish_prefs")

class PreferencesManager(private val context: Context) {

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] ?: false
        }

    val selectedModelId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SELECTED_MODEL]
        }

    val hasVisionModel: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_HAS_VISION] ?: false
        }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setSelectedModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODEL] = modelId
        }
    }

    suspend fun setHasVisionModel(hasVision: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_VISION] = hasVision
        }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model_id")
        private val KEY_HAS_VISION = booleanPreferencesKey("has_vision_model")
    }
}
