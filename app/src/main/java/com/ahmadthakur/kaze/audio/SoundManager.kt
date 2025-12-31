package com.ahmadthakur.kaze.audio

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SoundManager(private val context: Context?) {

    private val sounds = mutableMapOf<String, Sound>()
    private val players = mutableMapOf<String, SoundPlayer>()

    // Exposed observable count of active players for UI binding
    var activeCount by mutableStateOf(0)
        private set

    companion object {
        private const val TAG = "SoundManager"
        private const val PREFS_NAME = "kaze_prefs"
    }

    // Register sounds at startup
    fun registerSound(sound: Sound) {
        // load stored volume if available
        try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = "volume_${sound.id}"
            val saved = prefs?.getFloat(key, sound.volume) ?: sound.volume
            sound.volume = saved
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load saved volume for ${sound.id}", e)
        }

        // ensure playing is false initially
        sound.playing = false

        sounds[sound.id] = sound
        Log.d(TAG, "Registered sound: ${sound.id} (volume=${sound.volume})")
    }

    // Enable / start a sound
    fun enableSound(id: String) {
        val sound = sounds[id] ?: run {
            Log.w(TAG, "enableSound: unknown id=$id")
            return
        }
        if (players.containsKey(id)) {
            Log.d(TAG, "Player already exists for $id")
            return
        }
        // mark enabled in the model
        sound.enabled = true
        Log.d(TAG, "Enabling sound: $id")

        val player = SoundPlayer(context, sound,
            onStarted = {
                // called when MediaPlayer actually starts playback
                activeCount = activeCount + 1
            },
            onStopped = {
                // called when MediaPlayer stops
                activeCount = (activeCount - 1).coerceAtLeast(0)
            }
        )
        players[id] = player
        player.start()
    }

    // Stop / disable a sound
    fun disableSound(id: String) {
        Log.d(TAG, "Disabling sound: $id")
        players[id]?.stop()
        players.remove(id)
        sounds[id]?.enabled = false
        sounds[id]?.playing = false
        // activeCount will be updated via onStopped callback when the player actually stops
    }

    // Change volume for a playing sound
    fun setVolume(id: String, volume: Float) {
        Log.d(TAG, "Set volume for $id -> $volume")
        sounds[id]?.volume = volume
        players[id]?.setVolume(volume)
        try {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.edit()?.putFloat("volume_$id", volume)?.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist volume for $id", e)
        }
    }

    // Stop all sounds
    fun stopAll() {
        Log.d(TAG, "Stopping all sounds")
        players.values.forEach { it.stop() }
        players.clear()
        sounds.values.forEach {
            it.enabled = false
            it.playing = false
        }
        // activeCount will be adjusted by onStopped callbacks; ensure it is zero as a fallback
        activeCount = 0
    }

    // Get list of all sounds
    fun getAllSounds(): List<Sound> = sounds.values.toList()
}
