package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.audio.SoundEngine
import com.example.data.GamePreferences
import com.example.model.*
import com.example.ui.*
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.HyperRushTheme

class MainActivity : ComponentActivity() {
    private lateinit var soundEngine: SoundEngine
    private lateinit var prefs: GamePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = GamePreferences(this)
        soundEngine = SoundEngine()
        soundEngine.setMuted(!prefs.isSoundEnabled)
        soundEngine.setMusicMuted(!prefs.isMusicEnabled)
        soundEngine.setVolume(prefs.volume)

        setContent {
            HyperRushTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavyBg
                ) {
                    HyperRushApp(prefs = prefs, soundEngine = soundEngine)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::soundEngine.isInitialized && prefs.isMusicEnabled) {
            soundEngine.startMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::soundEngine.isInitialized) {
            soundEngine.stopMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundEngine.isInitialized) {
            soundEngine.release()
        }
    }
}

@Composable
fun HyperRushApp(
    prefs: GamePreferences,
    soundEngine: SoundEngine
) {
    var currentScreen by remember { mutableStateOf(ScreenState.MAIN_MENU) }
    var selectedWorld by remember { mutableStateOf(WorldRegistry.allWorlds.first()) }
    var selectedLevel by remember { mutableStateOf(selectedWorld.getLevels().first()) }

    // Start background music when appropriate
    LaunchedEffect(currentScreen) {
        if (prefs.isMusicEnabled) {
            soundEngine.startMusic()
        }
    }

    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
        when (screen) {
            ScreenState.MAIN_MENU -> {
                MainMenuScreen(
                    prefs = prefs,
                    soundEngine = soundEngine,
                    onRaceClick = {
                        val vehId = prefs.getSelectedVehicleId()
                        selectedLevel = selectedWorld.getLevels().first()
                        currentScreen = ScreenState.PLAYING
                    },
                    onGarageClick = {
                        currentScreen = ScreenState.GARAGE
                    },
                    onWorldsClick = {
                        currentScreen = ScreenState.WORLD_SELECT
                    }
                )
            }

            ScreenState.GARAGE -> {
                BackHandler {
                    currentScreen = ScreenState.MAIN_MENU
                }
                GarageScreen(
                    prefs = prefs,
                    soundEngine = soundEngine,
                    onNavigateBack = {
                        currentScreen = ScreenState.MAIN_MENU
                    },
                    onStartRace = {
                        selectedLevel = selectedWorld.getLevels().first()
                        currentScreen = ScreenState.PLAYING
                    }
                )
            }

            ScreenState.WORLD_SELECT -> {
                BackHandler {
                    currentScreen = ScreenState.MAIN_MENU
                }
                WorldSelectScreen(
                    prefs = prefs,
                    soundEngine = soundEngine,
                    onNavigateBack = {
                        currentScreen = ScreenState.MAIN_MENU
                    },
                    onSelectLevel = { world, level ->
                        selectedWorld = world
                        selectedLevel = level
                        currentScreen = ScreenState.PLAYING
                    }
                )
            }

            ScreenState.PLAYING -> {
                BackHandler {
                    currentScreen = ScreenState.MAIN_MENU
                }
                val currentVeh = remember(prefs.getSelectedVehicleId()) {
                    VehicleRegistry.getById(prefs.getSelectedVehicleId())
                }
                GameScreen(
                    vehicleType = currentVeh,
                    worldInfo = selectedWorld,
                    levelInfo = selectedLevel,
                    prefs = prefs,
                    soundEngine = soundEngine,
                    onNavigateBack = {
                        currentScreen = ScreenState.MAIN_MENU
                    },
                    onNextLevel = {
                        // Advance to next level
                        val levels = selectedWorld.getLevels()
                        val currentIndex = levels.indexOfFirst { it.id == selectedLevel.id }
                        if (currentIndex != -1 && currentIndex < levels.size - 1) {
                            selectedLevel = levels[currentIndex + 1]
                        } else {
                            // Advance to next world
                            val worldIndex = WorldRegistry.allWorlds.indexOfFirst { it.id == selectedWorld.id }
                            if (worldIndex != -1 && worldIndex < WorldRegistry.allWorlds.size - 1) {
                                selectedWorld = WorldRegistry.allWorlds[worldIndex + 1]
                                selectedLevel = selectedWorld.getLevels().first()
                            }
                        }
                    },
                    onOpenGarage = {
                        currentScreen = ScreenState.GARAGE
                    }
                )
            }

            else -> {
                MainMenuScreen(
                    prefs = prefs,
                    soundEngine = soundEngine,
                    onRaceClick = { currentScreen = ScreenState.PLAYING },
                    onGarageClick = { currentScreen = ScreenState.GARAGE },
                    onWorldsClick = { currentScreen = ScreenState.WORLD_SELECT }
                )
            }
        }
    }
}
