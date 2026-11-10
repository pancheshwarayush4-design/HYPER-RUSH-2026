package com.example.model

enum class ScreenState {
    MAIN_MENU,
    GARAGE,
    WORLD_SELECT,
    PLAYING,
    PAUSED,
    LEVEL_COMPLETE,
    GAME_OVER
}

data class StuntPopup(
    val title: String,
    val bonus: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class HudTelemetry(
    var speedKmh: Float = 0f,
    var rpm: Float = 1200f,
    var fuelRatio: Float = 1.0f,
    var nitroRatio: Float = 1.0f,
    var isNitroActive: Boolean = false,
    var distanceMeters: Float = 0f,
    var trackLengthMeters: Float = 500f,
    var coinsCollected: Int = 0,
    var diamondsCollected: Int = 0,
    var starsEarned: Int = 0,
    var currentStunt: StuntPopup? = null,
    var comboMultiplier: Int = 1,
    var comboTimer: Float = 0f,
    var timeElapsedSeconds: Float = 0f,
    var isLowFuel: Boolean = false
)

data class LevelResult(
    val levelId: String,
    val isVictory: Boolean,
    val distanceReached: Float,
    val targetDistance: Float,
    val coinsEarned: Int,
    val diamondsEarned: Int,
    val starsEarned: Int,
    val stuntsPerformed: Int,
    val timeSeconds: Float,
    val failReason: String? = null
)
