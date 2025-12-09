/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ishabdullah.aiish.data.local.preferences.PreferencesManager
import com.ishabdullah.aiish.ml.DownloadProgress
import com.ishabdullah.aiish.ml.ModelCatalog
import com.ishabdullah.aiish.ml.ModelInfo
import com.ishabdullah.aiish.ml.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class ModelDownloadState(
    val selectedModel: ModelInfo? = null,
    val includeVision: Boolean = false,
    val isDownloading: Boolean = false,
    val currentDownload: DownloadProgress? = null,
    val downloadComplete: Boolean = false,
    val error: String? = null
)

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val modelManager = ModelManager(application)
    private val preferencesManager = PreferencesManager(application)

    private val _state = MutableStateFlow(ModelDownloadState())
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            modelManager.downloadProgress.collect { progress ->
                _state.value = _state.value.copy(
                    currentDownload = progress,
                    isDownloading = progress != null && !progress.isComplete,
                    error = progress?.error
                )
            }
        }
    }

    fun selectModel(modelInfo: ModelInfo) {
        _state.value = _state.value.copy(selectedModel = modelInfo)
    }

    fun toggleVision(include: Boolean) {
        _state.value = _state.value.copy(includeVision = include)
    }

    fun startDownload() {
        val selectedModel = _state.value.selectedModel ?: return

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isDownloading = true, error = null)

                val result = modelManager.downloadModel(selectedModel)
                if (result.isFailure) {
                    _state.value = _state.value.copy(
                        error = "Download failed: ${result.exceptionOrNull()?.message}",
                        isDownloading = false
                    )
                    return@launch
                }

                if (_state.value.includeVision) {
                    val visionResult = modelManager.downloadModel(ModelCatalog.MOONDREAM2)
                    if (visionResult.isSuccess) {
                        preferencesManager.setHasVisionModel(true)
                    }
                }

                preferencesManager.setSelectedModel(selectedModel.id)
                preferencesManager.setOnboardingComplete(true)

                _state.value = _state.value.copy(
                    downloadComplete = true,
                    isDownloading = false
                )

                Timber.i("Model download complete, onboarding finished")

            } catch (e: Exception) {
                Timber.e(e, "Error during download")
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error",
                    isDownloading = false
                )
            }
        }
    }

    fun cancelDownload() {
        modelManager.cancelDownload()
        _state.value = _state.value.copy(isDownloading = false, currentDownload = null)
    }
}
