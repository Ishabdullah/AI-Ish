package com.ishabdullah.aiish.wake

import timber.log.Timber

/**
 * WakeWordManager - Detects "Hey Ish" wake phrase
 * Ported from AILive with phonetic matching
 */
class WakeWordManager(
    private var aiName: String = "Ish"
) {

    private var wakePhrase: String = generateWakePhrase(aiName)
    private var wakeAlternatives: List<String> = generateAlternatives(aiName)

    var onWakeWordDetected: (() -> Unit)? = null

    /**
     * Check if text contains wake word
     */
    fun processText(text: String): Boolean {
        val normalized = text.lowercase().trim()

        // Check exact match
        if (normalized.contains(wakePhrase.lowercase())) {
            Timber.i("🎯 Wake word detected (exact): '$text'")
            triggerWakeWordResponse()
            return true
        }

        // Check just the AI name without "hey"
        if (normalized.contains(aiName.lowercase())) {
            Timber.i("🎯 Wake word detected (name only): '$text'")
            triggerWakeWordResponse()
            return true
        }

        // Check alternatives
        for (alt in wakeAlternatives) {
            if (normalized.contains(alt)) {
                Timber.i("🎯 Wake word detected (alt: '$alt'): '$text'")
                triggerWakeWordResponse()
                return true
            }
        }

        return false
    }

    private fun triggerWakeWordResponse() {
        onWakeWordDetected?.invoke()
    }

    fun setAIName(name: String) {
        aiName = name
        wakePhrase = generateWakePhrase(name)
        wakeAlternatives = generateAlternatives(name)
        Timber.i("✅ AI name set to: '$name'")
    }

    fun getAIName(): String = aiName

    private fun generateWakePhrase(name: String): String {
        return "hey ${name.lowercase().trim()}"
    }

    private fun generateAlternatives(name: String): List<String> {
        val alts = mutableListOf<String>()
        val normalized = name.lowercase().trim()

        alts.add(normalized)

        // Common phonetic variations
        when {
            normalized.contains("ish") -> alts.addAll(listOf("is", "isha", "ishy"))
        }

        return alts.distinct()
    }
}
