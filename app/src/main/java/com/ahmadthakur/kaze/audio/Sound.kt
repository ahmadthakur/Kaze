package com.ahmadthakur.kaze.audio

data class Sound(
    val id: String,
    val label: String,
    val assetPath: String,
    var volume: Float = 1.0f,
    var enabled: Boolean = false
)
