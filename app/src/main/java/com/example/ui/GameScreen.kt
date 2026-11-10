package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.data.GamePreferences
import com.example.model.*
import com.example.physics.*
import com.example.ui.theme.*
import com.example.vfx.ParticleSystem
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun GameScreen(
    vehicleType: VehicleType,
    worldInfo: WorldInfo,
    levelInfo: LevelInfo,
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onNavigateBack: () -> Unit,
    onNextLevel: () -> Unit,
    onOpenGarage: () -> Unit
) {
    val particleSystem = remember { ParticleSystem() }
    val terrainGenerator = remember { TerrainGenerator() }
    var generatedLevel by remember(levelInfo.id) { mutableStateOf(terrainGenerator.generate(levelInfo)) }

    val upgradesMap = remember(vehicleType.id) {
        mapOf(
            "engine" to prefs.getUpgradeLevel(vehicleType.id, "engine"),
            "tires" to prefs.getUpgradeLevel(vehicleType.id, "tires"),
            "suspension" to prefs.getUpgradeLevel(vehicleType.id, "suspension"),
            "fuel" to prefs.getUpgradeLevel(vehicleType.id, "fuel"),
            "nitro" to prefs.getUpgradeLevel(vehicleType.id, "nitro")
        )
    }

    val physics = remember(vehicleType.id, worldInfo.id, levelInfo.id) {
        PhysicsEngine(
            vehicleType = vehicleType,
            upgrades = upgradesMap,
            world = worldInfo,
            level = generatedLevel,
            soundEngine = soundEngine,
            particleSystem = particleSystem,
            prefs = prefs
        )
    }

    // Input States
    var isGasPressed by remember { mutableStateOf(false) }
    var isBrakePressed by remember { mutableStateOf(false) }
    var isNitroPressed by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Camera smoothing
    var cameraX by remember { mutableFloatStateOf(physics.x) }
    var cameraY by remember { mutableFloatStateOf(physics.y) }
    var cameraZoom by remember { mutableFloatStateOf(16f) }

    // Result dialog states
    var showResultDialog by remember { mutableStateOf(false) }
    var resultData by remember { mutableStateOf<LevelResult?>(null) }

    // Game loop
    LaunchedEffect(isPaused, showResultDialog) {
        var lastTime = System.nanoTime()
        while (!isPaused && !showResultDialog) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000.0f).coerceIn(0.005f, 0.033f)
                lastTime = now

                val effectiveDt = (dt * particleSystem.bulletTimeScale).coerceIn(0.002f, 0.033f)

                val gas = if (isGasPressed) 1f else 0f
                val brake = if (isBrakePressed) 1f else 0f

                physics.update(effectiveDt, gas, brake, isNitroPressed)
                particleSystem.update(effectiveDt, physics.gravity)

                // Smooth Camera tracking with acceleration lead-ahead
                val leadAhead = physics.vx * 0.45f
                val shakeX = if (particleSystem.cameraShakeIntensity > 0f) {
                    (sin((now % 10000000L).toDouble() * 0.00005) * particleSystem.cameraShakeIntensity).toFloat()
                } else 0f
                val shakeY = if (particleSystem.cameraShakeIntensity > 0f) {
                    (cos((now % 10000000L).toDouble() * 0.00005) * particleSystem.cameraShakeIntensity).toFloat()
                } else 0f

                val targetCamX = physics.x + leadAhead + shakeX
                val targetCamY = physics.y - 1.2f + shakeY

                cameraX += (targetCamX - cameraX) * 0.12f
                cameraY += (targetCamY - cameraY) * 0.12f

                // Cinematic Air Cam & Dynamic speed zoom
                val speedZoom = (physics.hud.speedKmh * 0.07f)
                val airCamZoom = if (physics.airTime > 0.4f) 3.2f else 0f
                val targetZoom = (17.5f - speedZoom - airCamZoom).coerceIn(9.5f, 18.5f)
                cameraZoom += (targetZoom - cameraZoom) * 0.09f

                if (physics.isVictory && !showResultDialog) {
                    val stars = if (physics.hud.distanceMeters >= levelInfo.trackLengthMeters) {
                        maxOf(1, physics.hud.starsEarned)
                    } else 0
                    prefs.setLevelStars(levelInfo.id, stars)
                    prefs.addCoins(levelInfo.coinReward + physics.hud.coinsCollected)
                    prefs.addDiamonds(levelInfo.diamondReward + physics.hud.diamondsCollected)

                    resultData = LevelResult(
                        levelId = levelInfo.id,
                        isVictory = true,
                        distanceReached = physics.x,
                        targetDistance = levelInfo.trackLengthMeters,
                        coinsEarned = levelInfo.coinReward + physics.hud.coinsCollected,
                        diamondsEarned = levelInfo.diamondReward + physics.hud.diamondsCollected,
                        starsEarned = stars,
                        stuntsPerformed = physics.totalStuntsCount,
                        timeSeconds = physics.hud.timeElapsedSeconds
                    )
                    showResultDialog = true
                } else if (physics.isGameOver && !showResultDialog) {
                    prefs.addCoins(physics.hud.coinsCollected)
                    prefs.addDiamonds(physics.hud.diamondsCollected)

                    resultData = LevelResult(
                        levelId = levelInfo.id,
                        isVictory = false,
                        distanceReached = physics.x,
                        targetDistance = levelInfo.trackLengthMeters,
                        coinsEarned = physics.hud.coinsCollected,
                        diamondsEarned = physics.hud.diamondsCollected,
                        starsEarned = 0,
                        stuntsPerformed = physics.totalStuntsCount,
                        timeSeconds = physics.hud.timeElapsedSeconds,
                        failReason = physics.gameOverReason
                    )
                    showResultDialog = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavyBg)) {
        // Main World Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            GameRenderer.drawWorld(
                drawScope = this,
                physics = physics,
                level = generatedLevel,
                world = worldInfo,
                particles = particleSystem,
                cameraX = cameraX,
                cameraY = cameraY,
                zoom = cameraZoom,
                viewWidth = size.width,
                viewHeight = size.height
            )
        }

        // HUD Overlay
        GameHudOverlay(
            physics = physics,
            levelInfo = levelInfo,
            isGasPressed = isGasPressed,
            isBrakePressed = isBrakePressed,
            isNitroPressed = isNitroPressed,
            onGasChanged = { isGasPressed = it },
            onBrakeChanged = { isBrakePressed = it },
            onNitroChanged = { isNitroPressed = it },
            onPauseClick = { isPaused = true }
        )

        // Pause Menu Dialog
        if (isPaused) {
            PauseMenuDialog(
                soundEngine = soundEngine,
                prefs = prefs,
                onResume = { isPaused = false },
                onRestart = {
                    isPaused = false
                    generatedLevel = terrainGenerator.generate(levelInfo)
                    physics.reset(generatedLevel)
                },
                onExit = onNavigateBack
            )
        }

        // Result Dialog (Victory / Game Over)
        resultData?.let { res ->
            if (showResultDialog) {
                GameResultOverlay(
                    result = res,
                    onRetry = {
                        showResultDialog = false
                        resultData = null
                        generatedLevel = terrainGenerator.generate(levelInfo)
                        physics.reset(generatedLevel)
                    },
                    onNextLevel = onNextLevel,
                    onOpenGarage = onOpenGarage
                )
            }
        }
    }
}

@Composable
fun GameHudOverlay(
    physics: PhysicsEngine,
    levelInfo: LevelInfo,
    isGasPressed: Boolean,
    isBrakePressed: Boolean,
    isNitroPressed: Boolean,
    onGasChanged: (Boolean) -> Unit,
    onBrakeChanged: (Boolean) -> Unit,
    onNitroChanged: (Boolean) -> Unit,
    onPauseClick: () -> Unit
) {
    val hud = physics.hud

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BAR: Pause, Distance Progress, Collectibles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CardNavy.copy(alpha = 0.85f))
                    .border(1.5.dp, BorderCyan, CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pause Game", tint = Color.White)
            }

            // Distance Progress Bar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val progress = (hud.distanceMeters / levelInfo.trackLengthMeters).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${hud.distanceMeters.toInt()}m",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🏁 ${levelInfo.trackLengthMeters.toInt()}m",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonCyan,
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Coin & Diamond Counters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardNavy.copy(alpha = 0.85f))
                    .border(1.dp, BorderCyan, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("🪙", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${hud.coinsCollected}",
                    color = GoldCoin,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("💎", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${hud.diamondsCollected}",
                    color = DiamondBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // CENTER: Stunt Popups
        hud.currentStunt?.let { stunt ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.6f),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Surface(
                    color = NeonOrange.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NeonYellow),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stunt.title,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "+${stunt.bonus} PTS",
                            color = NeonYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // BOTTOM SECTION: Tachometer, Fuel Bar, Controls
        Column(modifier = Modifier.fillMaxWidth()) {
            // Speed & Fuel Dashboard Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Digital Speedometer & RPM
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardNavy.copy(alpha = 0.85f))
                        .border(1.dp, BorderCyan, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "${hud.speedKmh.toInt()}",
                            color = NeonCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "KM/H",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${hud.rpm.toInt()} RPM",
                            color = if (hud.rpm > 7000f) DangerRed else NeonYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { (hud.rpm / 8500f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(70.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (hud.rpm > 7000f) DangerRed else NeonYellow,
                            trackColor = Color(0xFF334155)
                        )
                    }
                }

                // Fuel Gauge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardNavy.copy(alpha = 0.85f))
                        .border(
                            1.dp,
                            if (hud.isLowFuel) DangerRed else BorderCyan,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (hud.isLowFuel) "⚠️ FUEL" else "⛽ FUEL",
                        color = if (hud.isLowFuel) DangerRed else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { hud.fuelRatio },
                        modifier = Modifier
                            .width(85.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (hud.fuelRatio > 0.4f) NeonGreen else if (hud.fuelRatio > 0.2f) NeonOrange else DangerRed,
                        trackColor = Color(0xFF334155)
                    )
                }
            }

            // CONTROLS ROW: Left: BRAKE, Center: NITRO, Right: GAS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // BRAKE / TILT BACK BUTTON
                PedalControl(
                    label = "BRAKE",
                    subLabel = "TILT BACK",
                    color = DangerRed,
                    isPressed = isBrakePressed,
                    onPressedChange = onBrakeChanged,
                    testTag = "brake_button"
                )

                // NITRO BOOST BUTTON (Center)
                NitroBoostButton(
                    nitroRatio = hud.nitroRatio,
                    isPressed = isNitroPressed,
                    onPressedChange = onNitroChanged
                )

                // GAS / ACCELERATE BUTTON
                PedalControl(
                    label = "GAS",
                    subLabel = "TILT FWD",
                    color = NeonGreen,
                    isPressed = isGasPressed,
                    onPressedChange = onGasChanged,
                    testTag = "gas_button"
                )
            }
        }
    }
}

@Composable
fun PedalControl(
    label: String,
    subLabel: String,
    color: Color,
    isPressed: Boolean,
    onPressedChange: (Boolean) -> Unit,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1.0f)

    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 75.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.7f))
                    } else {
                        listOf(CardNavy, Color(0xFF1E293B))
                    }
                )
            )
            .border(2.dp, if (isPressed) color else color.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPressedChange(true)
                        tryAwaitRelease()
                        onPressedChange(false)
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isPressed) Color.White else color,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subLabel,
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NitroBoostButton(
    nitroRatio: Float,
    isPressed: Boolean,
    onPressedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hasNitro = nitroRatio > 0.05f
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1.0f)

    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isPressed && hasNitro) {
                        listOf(NeonCyan, NitroFlame)
                    } else if (hasNitro) {
                        listOf(NeonCyan.copy(alpha = 0.8f), CardNavy)
                    } else {
                        listOf(Color.DarkGray, Color.Black)
                    }
                )
            )
            .border(2.5.dp, if (hasNitro) NeonCyan else Color.Gray, CircleShape)
            .pointerInput(hasNitro) {
                if (hasNitro) {
                    detectTapGestures(
                        onPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPressedChange(true)
                            tryAwaitRelease()
                            onPressedChange(false)
                        }
                    )
                }
            }
            .testTag("nitro_button"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚡", fontSize = 22.sp)
            Text(
                text = "NITRO",
                color = if (hasNitro) Color.White else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun PauseMenuDialog(
    soundEngine: SoundEngine,
    prefs: GamePreferences,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    var isSoundOn by remember { mutableStateOf(prefs.isSoundEnabled) }
    var isMusicOn by remember { mutableStateOf(prefs.isMusicEnabled) }

    AlertDialog(
        onDismissRequest = onResume,
        containerColor = CardNavy,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "GAME PAUSED",
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Audio Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound Effects", color = Color.White)
                    Switch(
                        checked = isSoundOn,
                        onCheckedChange = {
                            isSoundOn = it
                            prefs.isSoundEnabled = it
                            soundEngine.setMuted(!it)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background Synth", color = Color.White)
                    Switch(
                        checked = isMusicOn,
                        onCheckedChange = {
                            isMusicOn = it
                            prefs.isMusicEnabled = it
                            soundEngine.setMusicMuted(!it)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restart_button"),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderCyan)
                ) {
                    Text("RESTART LEVEL", color = Color.White)
                }
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().testTag("exit_button")
                ) {
                    Text("QUIT TO MENU", color = DangerRed)
                }
            }
        }
    )
}

@Composable
fun GameResultOverlay(
    result: LevelResult,
    onRetry: () -> Unit,
    onNextLevel: () -> Unit,
    onOpenGarage: () -> Unit
) {
    var displayCoins by remember { mutableIntStateOf(0) }

    LaunchedEffect(result.coinsEarned) {
        val target = result.coinsEarned
        val steps = 25
        val stepVal = maxOf(1, target / steps)
        for (i in 0..steps) {
            displayCoins = minOf(target, i * stepVal)
            delay(20)
        }
        displayCoins = target
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            color = CardNavy,
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(2.5.dp, if (result.isVictory) NeonGreen else DangerRed),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (result.isVictory) "🏆 VICTORY!" else "💥 CRASHED!",
                        color = if (result.isVictory) NeonGreen else DangerRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                result.failReason?.let { reason ->
                    Text(
                        text = reason,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Victory: 3-Star reveal & Rank
                if (result.isVictory) {
                    val rankText = when (result.starsEarned) {
                        3 -> "RANK S"
                        2 -> "RANK A"
                        else -> "RANK B"
                    }
                    val rankColor = when (result.starsEarned) {
                        3 -> NeonYellow
                        2 -> NeonCyan
                        else -> NeonOrange
                    }

                    Surface(
                        color = rankColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, rankColor),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            text = "⭐ $rankText ⭐",
                            color = rankColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (s in 1..3) {
                            Text(
                                text = "★",
                                fontSize = 36.sp,
                                color = if (s <= result.starsEarned) NeonYellow else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatBox(label = "DISTANCE", value = "${result.distanceReached.toInt()}m")
                    StatBox(label = "COINS", value = "+$displayCoins 🪙")
                    if (result.diamondsEarned > 0) {
                        StatBox(label = "DIAMONDS", value = "+${result.diamondsEarned} 💎")
                    } else {
                        StatBox(label = "STUNTS", value = "${result.stuntsPerformed}")
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (result.isVictory) {
                        Button(
                            onClick = onNextLevel,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("next_level_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("NEXT LEVEL ➔", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("retry_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (result.isVictory) CardNavy else NeonCyan
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (result.isVictory) androidx.compose.foundation.BorderStroke(1.5.dp, BorderCyan) else null
                    ) {
                        Text(
                            text = if (result.isVictory) "RETRY STAGE" else "QUICK RETRY ↻",
                            color = if (result.isVictory) Color.White else Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenGarage,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("result_garage_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderCyan)
                    ) {
                        Text("GARAGE & VEHICLE UPGRADES", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
