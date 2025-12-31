package com.ahmadthakur.kaze

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.ahmadthakur.kaze.R
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ahmadthakur.kaze.audio.Sound
import com.ahmadthakur.kaze.audio.SoundManager
import com.ahmadthakur.kaze.ui.theme.KazeTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)

        // Register all Kaze sounds
        val sounds = listOf(
            Sound("birds", "Birds", "audio/birds.ogg", R.drawable.birds_symbolic),
            Sound("boat", "Boat", "audio/boat.ogg", R.drawable.boat_symbolic),
            Sound("city", "City", "audio/city.ogg", R.drawable.city_symbolic),
            Sound("coffee_shop", "Coffee Shop", "audio/coffee-shop.ogg", R.drawable.coffee_shop_symbolic),
            Sound("fireplace", "Fireplace", "audio/fireplace.ogg", R.drawable.fireplace_symbolic),
            Sound("pink_noise", "Pink Noise", "audio/pink-noise.ogg", R.drawable.pink_noise_symbolic),
            Sound("rain", "Rain", "audio/rain.ogg", R.drawable.rain_symbolic),
            Sound("storm", "Storm", "audio/storm.ogg", R.drawable.storm_symbolic),
            Sound("stream", "Stream", "audio/stream.ogg", R.drawable.stream_symbolic),
            Sound("summer_night", "Summer Night", "audio/summer-night.ogg", R.drawable.summer_night_symbolic),
            Sound("train", "Train", "audio/train.ogg", R.drawable.train_symbolic),
            Sound("waves", "Waves", "audio/waves.ogg", R.drawable.waves_symbolic),
            Sound("white_noise", "White Noise", "audio/white-noise.ogg", R.drawable.white_noise_symbolic),
            Sound("wind", "Wind", "audio/wind.ogg", R.drawable.wind_symbolic)
        )


        sounds.forEach { soundManager.registerSound(it) }

        // Do NOT auto-start sounds; user will control playback via top FAB or per-item slider

        // Compose UI
        setContent {
            KazeTheme {
                var masterPlaying by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(text = "Kaze", style = MaterialTheme.typography.titleLarge)
                                    val playingCount = soundManager.activeCount
                                    Text(text = "Playing: $playingCount", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        )
                    },
                     // Floating action button is the single play/pause control now.
                     floatingActionButton = {
                         FloatingActionButton(onClick = {
                             val newState = !masterPlaying
                             masterPlaying = newState
                             Log.d(TAG, "FAB play/pause clicked -> $newState")
                             if (newState) {
                                 // start all sounds that have volume > 0
                                 sounds.filter { it.volume > 0f }.forEach {
                                     Log.d(TAG, "FAB play -> enabling ${it.id} (volume=${it.volume})")
                                     soundManager.enableSound(it.id)
                                 }
                             } else {
                                 Log.d(TAG, "FAB pause -> stopping all")
                                 soundManager.stopAll()
                             }
                         }) {
                             val icon = if (masterPlaying) R.drawable.ic_pause else R.drawable.ic_play
                             Image(
                                 painter = painterResource(id = icon),
                                 contentDescription = if (masterPlaying) "Pause all" else "Play enabled",
                                 modifier = Modifier.size(24.dp)
                             )
                         }
                     },
                 ) { innerPadding ->
                     Surface(
                         modifier = Modifier
                             .fillMaxSize()
                             .padding(innerPadding),
                         color = MaterialTheme.colorScheme.background
                     ) {
                         SoundListUI(sounds, soundManager, masterPlaying) {
                             // onStartRequested: set masterPlaying true when user raises a volume
                             if (!masterPlaying) {
                                 Log.d(TAG, "Start requested by volume change")
                                 masterPlaying = true
                                 sounds.filter { it.volume > 0f }.forEach { soundManager.enableSound(it.id) }
                             }
                         }
                     }
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
 fun SoundListUI(sounds: List<Sound>, soundManager: SoundManager, masterPlaying: Boolean, onStartRequested: () -> Unit) {
     Column(modifier = Modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState())
         .padding(16.dp)) {

         // Title moved to top app bar; keep a small spacer for breathing room
         Spacer(modifier = Modifier.height(8.dp))

         sounds.forEach { sound ->
             // Read observable states directly
             val volume = sound.volume

             Card(modifier = Modifier
                 .fillMaxWidth()
                 .padding(vertical = 6.dp)) {
                 Column(modifier = Modifier
                     .fillMaxWidth()
                     .padding(12.dp)) {

                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         if (sound.iconRes != 0) {
                             Image(
                                 painter = painterResource(id = sound.iconRes),
                                 contentDescription = sound.label,
                                 modifier = Modifier.size(40.dp),
                                 contentScale = ContentScale.Fit
                             )
                         } else {
                             Box(modifier = Modifier
                                 .size(40.dp)
                                 .background(Color.LightGray))
                         }

                         Spacer(modifier = Modifier.width(12.dp))

                         Column(modifier = Modifier.weight(1f)) {
                             Text(
                                 text = sound.label,
                                 style = MaterialTheme.typography.titleMedium
                             )
                         }
                     }

                     Spacer(modifier = Modifier.height(8.dp))

                     Slider(
                         value = volume,
                         onValueChange = { v ->
                             // update model and persist
                             sound.volume = v
                             soundManager.setVolume(sound.id, v)
                             if (v > 0f) {
                                 // start this sound immediately
                                 soundManager.enableSound(sound.id)
                                 // ensure master is playing
                                 onStartRequested()
                             } else {
                                 // slider set to 0 -> stop this sound
                                 soundManager.disableSound(sound.id)
                             }
                         },
                         valueRange = 0f..1f,
                         modifier = Modifier.fillMaxWidth()
                     )
                 }
             }
         }
     }
 }
