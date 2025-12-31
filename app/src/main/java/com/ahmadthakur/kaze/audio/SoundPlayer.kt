package com.ahmadthakur.kaze.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

class SoundPlayer(
    private val context: Context?,
    private val sound: Sound
) {
    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        if (mediaPlayer != null) return

        val ctx = context ?: run {
            Log.w(TAG, "Context is null; cannot start sound: ${sound.assetPath}")
            return
        }

        try {
            val afd = ctx.assets.openFd(sound.assetPath)
            afd.use { descriptor ->
                mediaPlayer = MediaPlayer().apply {
                    setOnErrorListener { mp, what, extra ->
                        Log.e(TAG, "MediaPlayer error what=$what extra=$extra for ${sound.assetPath}")
                        try {
                            mp.reset()
                            mp.release()
                        } catch (_: Exception) {
                        }
                        mediaPlayer = null
                        true
                    }

                    try {
                        setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                        isLooping = true
                        val vol = sound.volume.coerceIn(0f, 1f)
                        setVolume(vol, vol)
                        prepare()
                        start()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to prepare/start media player for ${sound.assetPath}", e)
                        try {
                            reset()
                            release()
                        } catch (_: Exception) {
                        }
                        mediaPlayer = null
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open asset ${sound.assetPath}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while starting sound ${sound.assetPath}", e)
        }
    }

    fun stop() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    try {
                        mp.stop()
                    } catch (_: IllegalStateException) {
                        // ignore stop errors
                    }
                }
            } finally {
                try {
                    mp.reset()
                } catch (_: Exception) {
                }
                try {
                    mp.release()
                } catch (_: Exception) {
                }
                mediaPlayer = null
            }
        }
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        sound.volume = v
        mediaPlayer?.setVolume(v, v)
    }

    companion object {
        private const val TAG = "SoundPlayer"
    }
}
