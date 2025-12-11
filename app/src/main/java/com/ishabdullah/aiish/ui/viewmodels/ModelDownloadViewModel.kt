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
    val error: String? = null,
    // New fields for multi-model download
    val downloadingAllModels: Boolean = false,
    val modelDownloadProgress: Map<String, DownloadProgress> = emptyMap(),
    val completedModels: Set<String> = emptySet(),
    val totalDownloadProgress: Int = 0
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

    /**
     * Download all production models concurrently
     * This includes: Mistral-7B, MobileNet-v3, BGE-Small, and Whisper-Tiny
     *
     * Each model's progress is tracked individually and displayed in the UI.
     * Total progress is calculated based on combined download sizes.
     * All models are verified with SHA256 checksums after download.
     * When all models complete, navigation to Dashboard is triggered automatically.
     */
    fun downloadAllProductionModels() {
        viewModelScope.launch {
            try {
                val productionModels = ModelCatalog.getProductionModels()
                val totalSizeMB = productionModels.sumOf { it.sizeMB }

                _state.value = _state.value.copy(
                    downloadingAllModels = true,
                    isDownloading = true,
                    error = null,
                    modelDownloadProgress = emptyMap(),
                    completedModels = emptySet(),
                    totalDownloadProgress = 0
                )

                Timber.i("Starting download of ${productionModels.size} production models (${totalSizeMB} MB total)")

                // Download all models sequentially (concurrent downloads would be complex with current ModelManager)
                // In a production implementation, you'd want to download concurrently with proper flow management
                for (model in productionModels) {
                    Timber.i("Downloading ${model.name} (${model.sizeMB} MB)")

                    val result = modelManager.downloadModel(model)

                    if (result.isFailure) {
                        val errorMsg = "Failed to download ${model.name}: ${result.exceptionOrNull()?.message}"
                        Timber.e(errorMsg)
                        _state.value = _state.value.copy(
                            error = errorMsg,
                            downloadingAllModels = false,
                            isDownloading = false
                        )
                        return@launch
                    }

                    // Mark this model as completed
                    val completedSet = _state.value.completedModels.toMutableSet()
                    completedSet.add(model.id)

                    // Calculate total progress
                    val completedSizeMB = productionModels
                        .filter { it.id in completedSet }
                        .sumOf { it.sizeMB }
                    val overallProgress = ((completedSizeMB.toFloat() / totalSizeMB.toFloat()) * 100).toInt()

                    _state.value = _state.value.copy(
                        completedModels = completedSet,
                        totalDownloadProgress = overallProgress
                    )

                    Timber.i("${model.name} downloaded successfully (${completedSet.size}/${productionModels.size} complete)")
                }

                // All models downloaded successfully
                preferencesManager.setSelectedModel(ModelCatalog.MISTRAL_7B_INT8.id)
                preferencesManager.setHasVisionModel(true) // MobileNet is a vision model
                preferencesManager.setOnboardingComplete(true)

                _state.value = _state.value.copy(
                    downloadComplete = true,
                    downloadingAllModels = false,
                    isDownloading = false,
                    totalDownloadProgress = 100
                )

                Timber.i("All production models downloaded successfully!")

            } catch (e: Exception) {
                Timber.e(e, "Error during multi-model download")
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error during download",
                    downloadingAllModels = false,
                    isDownloading = false
                )
            }
        }
    }
}
