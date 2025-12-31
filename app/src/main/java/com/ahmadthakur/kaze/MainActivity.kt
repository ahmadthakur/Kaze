package com.ahmadthakur.kaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahmadthakur.kaze.audio.Sound
import com.ahmadthakur.kaze.audio.SoundManager
import com.ahmadthakur.kaze.ui.theme.KazeTheme

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)

        // Register all Kaze sounds
        val sounds = listOf(
            Sound("birds", "Birds", "audio/birds.ogg"),
            Sound("boat", "Boat", "audio/boat.ogg"),
            Sound("city", "City", "audio/city.ogg"),
            Sound("coffee_shop", "Coffee Shop", "audio/coffee-shop.ogg"),
            Sound("fireplace", "Fireplace", "audio/fireplace.ogg"),
            Sound("pink_noise", "Pink Noise", "audio/pink-noise.ogg"),
            Sound("rain", "Rain", "audio/rain.ogg"),
            Sound("storm", "Storm", "audio/storm.ogg"),
            Sound("stream", "Stream", "audio/stream.ogg"),
            Sound("summer_night", "Summer Night", "audio/summer-night.ogg"),
            Sound("train", "Train", "audio/train.ogg"),
            Sound("waves", "Waves", "audio/waves.ogg"),
            Sound("white_noise", "White Noise", "audio/white-noise.ogg"),
            Sound("wind", "Wind", "audio/wind.ogg")
        )

        sounds.forEach { soundManager.registerSound(it) }

        // Start a couple of sounds as a test
        soundManager.enableSound("rain")
        soundManager.setVolume("rain", 0.6f)
        soundManager.enableSound("waves")
        soundManager.setVolume("waves", 0.4f)

        // Compose UI
        setContent {
            KazeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SoundListUI(sounds, soundManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.stopAll()
    }
}

@Composable
fun SoundListUI(sounds: List<Sound>, soundManager: SoundManager) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text(
            text = "Kaze Ambient Sounds",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        sounds.forEach { sound ->
            var isPlaying by remember { mutableStateOf(sound.enabled) }
            var volume by remember { mutableStateOf(sound.volume) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = sound.label)
                Switch(
                    checked = isPlaying,
                    onCheckedChange = {
                        isPlaying = it
                        if (it) soundManager.enableSound(sound.id)
                        else soundManager.disableSound(sound.id)
                    }
                )
            }

            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    soundManager.setVolume(sound.id, it)
                },
                valueRange = 0f..1f,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SoundListPreview() {
    KazeTheme {
        SoundListUI(
            sounds = listOf(
                Sound("rain", "Rain", ""),
                Sound("waves", "Waves", "")
            ),
            soundManager = SoundManager(null) // placeholder for preview
        )
    }
}
