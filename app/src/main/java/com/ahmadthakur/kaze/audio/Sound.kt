package com.ahmadthakur.kaze.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Sound(
    val id: String,
    val label: String,
    val assetPath: String,
    val iconRes: Int,
    private var _volume: Float = 0.0f,
    private var _enabled: Boolean = false
) {
    var volume by mutableStateOf(_volume)
    var enabled by mutableStateOf(_enabled)
    var playing by mutableStateOf(false)
}
