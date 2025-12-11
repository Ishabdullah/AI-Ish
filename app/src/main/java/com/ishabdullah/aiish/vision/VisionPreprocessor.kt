/*
 * Copyright (c) 2025 Ismail Abdullah. All rights reserved.
 *
 * This software and all associated files are the exclusive property of Ismail Abdullah.
 * Unauthorized use, copying, modification, or distribution is strictly prohibited.
 *
 * Contact: ismail.t.abdullah@gmail.com
 */

package com.ishabdullah.aiish.vision

import android.graphics.Bitmap
import android.graphics.Matrix
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VisionPreprocessor - Prepare images for vision model inference
 *
 * Ported from AILive's vision pipeline:
 * - Resize to model input size (960x960 for Qwen2-VL, 378x378 for Moondream2)
 * - Normalize pixel values to [-1, 1] or [0, 1]
 * - Convert to NCHW format (channels-first)
 * - Apply ImageNet mean/std normalization
 */
class VisionPreprocessor(
    private val modelType: VisionModelType = VisionModelType.MOONDREAM2
) {

    enum class VisionModelType {
        MOONDREAM2,  // 378x378, RGB, [-1, 1] normalization
        QWEN2_VL     // 960x960, RGB, ImageNet normalization
    }

    // ImageNet mean and std for normalization
    private val imagenetMean = floatArrayOf(0.485f, 0.456f, 0.406f)  // RGB
    private val imagenetStd = floatArrayOf(0.229f, 0.224f, 0.225f)   // RGB

    /**
     * Get required input size for the model
     */
    fun getInputSize(): Pair<Int, Int> {
        return when (modelType) {
            VisionModelType.MOONDREAM2 -> Pair(378, 378)
            VisionModelType.QWEN2_VL -> Pair(960, 960)
        }
    }

    /**
     * Preprocess bitmap for vision model inference
     * Returns FloatArray in NCHW format (channels, height, width)
     */
    fun preprocess(bitmap: Bitmap): FloatArray {
        val (targetWidth, targetHeight) = getInputSize()

        // Step 1: Resize image
        val resizedBitmap = resizeWithPadding(bitmap, targetWidth, targetHeight)

        // Step 2: Extract pixels
        val pixels = IntArray(targetWidth * targetHeight)
        resizedBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        // Step 3: Convert to normalized float array in NCHW format
        val normalizedPixels = when (modelType) {
            VisionModelType.MOONDREAM2 -> normalizeMoondream(pixels, targetWidth, targetHeight)
            VisionModelType.QWEN2_VL -> normalizeImageNet(pixels, targetWidth, targetHeight)
        }

        // Cleanup
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        Timber.d("Preprocessed image: ${targetWidth}x${targetHeight}, type=$modelType")
        return normalizedPixels
    }

    /**
     * Preprocess and return as ByteBuffer (for some ONNX models)
     */
    fun preprocessToBuffer(bitmap: Bitmap): ByteBuffer {
        val floatArray = preprocess(bitmap)
        val buffer = ByteBuffer.allocateDirect(floatArray.size * 4) // 4 bytes per float
        buffer.order(ByteOrder.nativeOrder())

        val floatBuffer = buffer.asFloatBuffer()
        floatBuffer.put(floatArray)
        buffer.position(0)

        return buffer
    }

    /**
     * Resize image with padding to maintain aspect ratio
     */
    private fun resizeWithPadding(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calculate scale to fit within target size
        val scale = minOf(
            targetWidth.toFloat() / originalWidth,
            targetHeight.toFloat() / originalHeight
        )

        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        // Create scaled bitmap
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)

        // Create target bitmap with padding
        val paddedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(paddedBitmap)

        // Fill with black background
        canvas.drawColor(android.graphics.Color.BLACK)

        // Center the scaled image
        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, null)

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return paddedBitmap
    }

    /**
     * Normalize for Moondream2: [-1, 1] range
     */
    private fun normalizeMoondream(pixels: IntArray, width: Int, height: Int): FloatArray {
        val size = width * height
        val output = FloatArray(size * 3) // RGB channels

        for (i in pixels.indices) {
            val pixel = pixels[i]

            // Extract RGB (skip alpha)
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            // Normalize to [-1, 1]
            output[i] = r * 2f - 1f                    // R channel
            output[size + i] = g * 2f - 1f             // G channel
            output[size * 2 + i] = b * 2f - 1f         // B channel
        }

        return output
    }

    /**
     * Normalize with ImageNet mean/std (for Qwen2-VL)
     */
    private fun normalizeImageNet(pixels: IntArray, width: Int, height: Int): FloatArray {
        val size = width * height
        val output = FloatArray(size * 3) // RGB channels

        for (i in pixels.indices) {
            val pixel = pixels[i]

            // Extract RGB (skip alpha)
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            // Apply ImageNet normalization: (x - mean) / std
            output[i] = (r - imagenetMean[0]) / imagenetStd[0]                    // R channel
            output[size + i] = (g - imagenetMean[1]) / imagenetStd[1]             // G channel
            output[size * 2 + i] = (b - imagenetMean[2]) / imagenetStd[2]         // B channel
        }

        return output
    }

    /**
     * Get number of input elements (for buffer allocation)
     */
    fun getInputElementCount(): Int {
        val (width, height) = getInputSize()
        return width * height * 3 // RGB channels
    }
}
