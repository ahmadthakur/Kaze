package com.ahmadthakur.kaze.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SoundPlayer(
    private val context: Context?,
    private val sound: Sound,
    private val onStarted: (() -> Unit)? = null,
    private val onStopped: (() -> Unit)? = null
) {
    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null
    private var assetFd: AssetFileDescriptor? = null

    fun start() {
        if (mediaPlayer != null) return

        val ctx = context ?: run {
            Log.w(TAG, "Context is null; cannot start sound: ${sound.assetPath}")
            return
        }

        // Run heavy I/O and MediaPlayer setup off the main thread
        Thread {
            try {
                try {
                    // Try the fast path: AssetFileDescriptor (works when asset is uncompressed)
                    val afd = ctx.assets.openFd(sound.assetPath)
                    // keep afd open until prepared
                    assetFd = afd
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )

                        setOnErrorListener { mp, what, extra ->
                            Log.e(TAG, "MediaPlayer error what=$what extra=$extra for ${sound.assetPath}")
                            try {
                                mp.reset()
                                mp.release()
                            } catch (_: Exception) {}
                            // ensure playing flag cleared and notify stopped
                            sound.playing = false
                            onStopped?.invoke()
                            true
                        }

                        setOnPreparedListener { mp ->
                            // ensure start happens on main thread
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    mp.start()
                                    Log.d(TAG, "Prepared and started playback for ${sound.id} from asset")
                                    sound.playing = true
                                    onStarted?.invoke()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to start after prepared for ${sound.id}", e)
                                } finally {
                                    // close the asset descriptor now that media player has prepared
                                    try {
                                        assetFd?.close()
                                    } catch (_: Exception) {}
                                    assetFd = null
                                }
                            }
                        }

                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        isLooping = true
                        val vol = sound.volume.coerceIn(0f, 1f)
                        setVolume(vol, vol)
                        prepareAsync()
                    }

                    mediaPlayer = player
                } catch (e: Exception) {
                    // Fallback: asset may be compressed; copy to temp file and play from file path
                    Log.i(TAG, "Falling back to copy-play for ${sound.assetPath}: ${e.message}")
                    val input = ctx.assets.open(sound.assetPath)
                    tempFile = File.createTempFile("kaze_", ".ogg", ctx.cacheDir).apply { deleteOnExit() }
                    FileOutputStream(tempFile!!).use { out ->
                        input.copyTo(out)
                    }
                    input.close()

                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )

                        setOnErrorListener { mp, what, extra ->
                            Log.e(TAG, "MediaPlayer error what=$what extra=$extra for ${sound.assetPath}")
                            try {
                                mp.reset()
                                mp.release()
                            } catch (_: Exception) {}
                            sound.playing = false
                            onStopped?.invoke()
                            true
                        }

                        setOnPreparedListener { mp ->
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    mp.start()
                                    Log.d(TAG, "Prepared and started playback for ${sound.id} from temp file")
                                    sound.playing = true
                                    onStarted?.invoke()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to start after prepared for ${sound.id}", e)
                                }
                            }
                        }

                        try {
                            setDataSource(tempFile!!.absolutePath)
                            isLooping = true
                            val vol = sound.volume.coerceIn(0f, 1f)
                            setVolume(vol, vol)
                            prepareAsync()
                        } catch (ex: Exception) {
                            Log.e(TAG, "Failed to prepare async from temp file for ${sound.assetPath}", ex)
                            try {
                                reset()
                                release()
                            } catch (_: Exception) {}
                        }
                    }

                    mediaPlayer = player
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to open asset ${sound.assetPath}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error while starting sound ${sound.assetPath}", e)
            }
        }.start()
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
                } catch (_: Exception) {}
                try {
                    mp.release()
                } catch (_: Exception) {}
                Log.d(TAG, "Stopped playback for ${sound.id}")
                if (sound.playing) {
                    sound.playing = false
                    onStopped?.invoke()
                }
                mediaPlayer = null
                // delete temp file if used
                try {
                    tempFile?.delete()
                } catch (_: Exception) {}
                tempFile = null
                // close asset descriptor if still open
                try {
                    assetFd?.close()
                } catch (_: Exception) {}
                assetFd = null
            }
        }
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        sound.volume = v
        try {
            mediaPlayer?.setVolume(v, v)
        } catch (e: Exception) {
            Log.w(TAG, "setVolume failed for ${sound.id}", e)
        }
    }

    companion object {
        private const val TAG = "SoundPlayer"
    }
}
