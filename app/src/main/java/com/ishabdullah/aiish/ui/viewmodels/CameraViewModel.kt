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
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ishabdullah.aiish.vision.VisionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class CameraState(
    val isVisionAvailable: Boolean = false,
    val isProcessing: Boolean = false,
    val currentDescription: String = "",
    val error: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val visionManager = VisionManager(application)

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val available = visionManager.initialize()
            _state.value = _state.value.copy(isVisionAvailable = available)
        }

        viewModelScope.launch {
            visionManager.isProcessing.collect { processing ->
                _state.value = _state.value.copy(isProcessing = processing)
            }
        }

        viewModelScope.launch {
            visionManager.lastResult.collect { result ->
                result?.let {
                    _state.value = _state.value.copy(currentDescription = it.description)
                }
            }
        }
    }

    fun analyzeFrame(bitmap: Bitmap) {
        if (_state.value.isProcessing) return

        viewModelScope.launch {
            try {
                val result = visionManager.analyzeImage(bitmap)
                _state.value = _state.value.copy(
                    currentDescription = result.description,
                    error = null
                )
            } catch (e: Exception) {
                Timber.e(e, "Error analyzing frame")
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        visionManager.release()
    }
}
