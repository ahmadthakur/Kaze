package com.ahmadthakur.kaze.audio

import android.content.Context

class SoundManager(private val context: Context?) {

    private val sounds = mutableMapOf<String, Sound>()
    private val players = mutableMapOf<String, SoundPlayer>()

    // Register sounds at startup
    fun registerSound(sound: Sound) {
        sounds[sound.id] = sound
    }

    // Enable / start a sound
    fun enableSound(id: String) {
        val sound = sounds[id] ?: return
        if (sound.enabled) return
        sound.enabled = true

        val player = SoundPlayer(context, sound)
        players[id] = player
        player.start()
    }

    // Stop / disable a sound
    fun disableSound(id: String) {
        players[id]?.stop()
        players.remove(id)
        sounds[id]?.enabled = false
    }

    // Change volume for a playing sound
    fun setVolume(id: String, volume: Float) {
        sounds[id]?.volume = volume
        players[id]?.setVolume(volume)
    }

    // Stop all sounds
    fun stopAll() {
        players.values.forEach { it.stop() }
        players.clear()
        sounds.values.forEach { it.enabled = false }
    }

    // Get list of all sounds
    fun getAllSounds(): List<Sound> = sounds.values.toList()
}
