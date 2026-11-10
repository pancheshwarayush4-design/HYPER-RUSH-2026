package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.data.GamePreferences
import com.example.data.PlayerProfile
import com.example.model.VehicleRegistry
import com.example.model.WorldRegistry
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onRaceClick: () -> Unit,
    onGarageClick: () -> Unit,
    onWorldsClick: () -> Unit
) {
    val coins by prefs.coinsFlow.collectAsState()
    val diamonds by prefs.diamondsFlow.collectAsState()
    val totalStars = remember { prefs.getTotalStarsEarned() }
    val canClaimDaily = remember { prefs.canClaimDailyReward() }

    var showDailyRewardDialog by remember { mutableStateOf(false) }
    var showMissionsDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    var profile by remember { mutableStateOf(prefs.getProfile()) }

    Scaffold(
        containerColor = DarkNavyBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Profile & Currencies
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardNavy)
                        .border(1.dp, BorderCyan, RoundedCornerShape(20.dp))
                        .clickable {
                            soundEngine.playButtonClick()
                            showProfileDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("profile_button")
                ) {
                    val avatars = listOf("🏎️", "🚀", "⚡", "🔥", "👑", "🏁")
                    Text(
                        text = avatars.getOrElse(profile.avatarIndex) { "🏎️" },
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profile.username,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Currency Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardNavy)
                        .border(1.dp, BorderCyan, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("🪙 $coins", color = GoldCoin, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("💎 $diamonds", color = DiamondBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TITLE HERO BANNER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardNavy,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan),
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F1E36), CardNavy)
                            )
                        )
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HYPER RUSH",
                        color = NeonCyan,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "2026 EDITION",
                        color = NeonOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⭐ $totalStars STARS",
                                color = NeonYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val activeVeh = VehicleRegistry.getById(prefs.getSelectedVehicleId())
                            Text(
                                text = "🚗 ${activeVeh.name}",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRIMARY BIG ACTION BUTTON: "RACE NOW"
            Button(
                onClick = {
                    soundEngine.playButtonClick()
                    onRaceClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("main_race_now_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🏁 RACE NOW",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECONDARY ACTIONS GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Garage & Vehicles
                MenuCard(
                    title = "GARAGE",
                    subtitle = "12 Vehicles",
                    icon = "🚗",
                    color = NeonOrange,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_garage_button",
                    onClick = {
                        soundEngine.playButtonClick()
                        onGarageClick()
                    }
                )

                // Worlds & Levels
                MenuCard(
                    title = "WORLDS",
                    subtitle = "100 Levels",
                    icon = "🌍",
                    color = NeonCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_worlds_button",
                    onClick = {
                        soundEngine.playButtonClick()
                        onWorldsClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Daily Rewards
                MenuCard(
                    title = "DAILY GIFTS",
                    subtitle = if (canClaimDaily) "Ready to Claim!" else "Streak: ${prefs.getDailyStreak()}d",
                    icon = "🎁",
                    color = NeonYellow,
                    hasBadge = canClaimDaily,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_daily_button",
                    onClick = {
                        soundEngine.playButtonClick()
                        showDailyRewardDialog = true
                    }
                )

                // Missions
                MenuCard(
                    title = "MISSIONS",
                    subtitle = "Daily Quests",
                    icon = "🎯",
                    color = NeonPink,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_missions_button",
                    onClick = {
                        soundEngine.playButtonClick()
                        showMissionsDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Leaderboards
                MenuCard(
                    title = "RANKS",
                    subtitle = "Leaderboards",
                    icon = "🏆",
                    color = GoldCoin,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_leaderboard_button",
                    onClick = {
                        soundEngine.playButtonClick()
                        showLeaderboardDialog = true
                    }
                )

                // Settings
                MenuCard(
                    title = "SETTINGS",
                    subtitle = "Audio & Controls",
                    icon = "⚙️",
                    color = Color.LightGray,
                    modifier = Modifier.weight(1f),
                    testTag = "menu_settings_button",
                    onClick = {
                        soundEngine.playButtonClick()
                        showSettingsDialog = true
                    }
                )
            }
        }

        // DIALOGS
        if (showDailyRewardDialog) {
            DailyRewardDialog(
                prefs = prefs,
                soundEngine = soundEngine,
                onDismiss = { showDailyRewardDialog = false }
            )
        }

        if (showMissionsDialog) {
            MissionsDialog(
                prefs = prefs,
                soundEngine = soundEngine,
                onDismiss = { showMissionsDialog = false }
            )
        }

        if (showLeaderboardDialog) {
            LeaderboardDialog(
                prefs = prefs,
                onDismiss = { showLeaderboardDialog = false }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                prefs = prefs,
                soundEngine = soundEngine,
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showProfileDialog) {
            ProfileDialog(
                prefs = prefs,
                onDismiss = {
                    profile = prefs.getProfile()
                    showProfileDialog = false
                }
            )
        }
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    hasBadge: Boolean = false,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = CardNavy,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(icon, fontSize = 24.sp)
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                )
            }
        }
    }
}

@Composable
fun DailyRewardDialog(
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onDismiss: () -> Unit
) {
    val streak = prefs.getDailyStreak()
    val canClaim = prefs.canClaimDailyReward()
    var claimedReward by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "🎁 DAILY LOGIN REWARDS",
                color = NeonYellow,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Streak: Day $streak of 7",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 7 Days Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (d in 1..7) {
                        val isToday = d == streak
                        val isDone = d < streak
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            color = if (isToday) Color(0xFF1E3A5F) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isToday) 1.5.dp else 0.5.dp,
                                if (isToday) NeonYellow else Color.DarkGray
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("D$d", fontSize = 10.sp, color = Color.LightGray)
                                Text(if (d % 3 == 0) "💎" else "🪙", fontSize = 14.sp)
                                Text(
                                    text = if (isDone) "✓" else "+${d * 300}",
                                    fontSize = 9.sp,
                                    color = if (isDone) NeonGreen else NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                claimedReward?.let { reward ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Claimed +$reward Coins!",
                        color = NeonGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            if (canClaim && claimedReward == null) {
                Button(
                    onClick = {
                        val rew = prefs.claimDailyReward()
                        soundEngine.playVictory()
                        claimedReward = rew
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("claim_daily_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow)
                ) {
                    Text("CLAIM TODAY'S REWARD", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun MissionsDialog(
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onDismiss: () -> Unit
) {
    val missions = remember {
        listOf(
            "Drive 2,000 meters in any world" to "🪙 +500",
            "Perform 5 Flips in mid-air" to "💎 +3",
            "Reach 100 km/h with Nitro" to "🪙 +750",
            "Collect 3 Stars in a single run" to "💎 +5"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "🎯 DAILY MISSIONS",
                color = NeonPink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                missions.forEach { (task, reward) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("In Progress (Active)", color = NeonCyan, fontSize = 10.sp)
                            }
                            Text(reward, color = NeonYellow, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun LeaderboardDialog(
    prefs: GamePreferences,
    onDismiss: () -> Unit
) {
    val profile = remember { prefs.getProfile() }

    val mockRivals = listOf(
        Triple("HyperSonic_99", "12,450m", "48★"),
        Triple("NeonRider_X", "10,210m", "42★"),
        Triple(profile.username, "${(profile.totalDistanceKm * 1000).toInt()}m", "${prefs.getTotalStarsEarned()}★"),
        Triple("TurboGhost", "8,920m", "35★"),
        Triple("VortexViper", "6,150m", "28★")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "🏆 GLOBAL LEADERBOARD",
                color = GoldCoin,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mockRivals.forEachIndexed { idx, (name, dist, stars) ->
                    val isUser = name == profile.username
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isUser) Color(0xFF1E3A5F) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isUser) androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${idx + 1}",
                                    color = if (idx == 0) GoldCoin else Color.LightGray,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = name,
                                    color = if (isUser) NeonCyan else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dist, color = Color.LightGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stars, color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onDismiss: () -> Unit
) {
    var isSoundOn by remember { mutableStateOf(prefs.isSoundEnabled) }
    var isMusicOn by remember { mutableStateOf(prefs.isMusicEnabled) }
    var vol by remember { mutableFloatStateOf(prefs.volume) }
    var hapticOn by remember { mutableStateOf(prefs.isHapticsEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "⚙️ GAME SETTINGS",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                    Text("Background Synth Music", color = Color.White)
                    Switch(
                        checked = isMusicOn,
                        onCheckedChange = {
                            isMusicOn = it
                            prefs.isMusicEnabled = it
                            soundEngine.setMusicMuted(!it)
                        }
                    )
                }

                Column {
                    Text("Master Volume: ${(vol * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = vol,
                        onValueChange = {
                            vol = it
                            prefs.volume = it
                            soundEngine.setVolume(it)
                        },
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Feedback", color = Color.White)
                    Switch(
                        checked = hapticOn,
                        onCheckedChange = {
                            hapticOn = it
                            prefs.isHapticsEnabled = it
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ProfileDialog(
    prefs: GamePreferences,
    onDismiss: () -> Unit
) {
    val prof = remember { prefs.getProfile() }
    var username by remember { mutableStateOf(prof.username) }
    var avatarIdx by remember { mutableIntStateOf(prof.avatarIndex) }

    val avatars = listOf("🏎️", "🚀", "⚡", "🔥", "👑", "🏁")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardNavy,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "PLAYER PROFILE",
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Picker
                Text("Choose Avatar:", color = Color.LightGray, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    avatars.forEachIndexed { i, a ->
                        val isSelected = i == avatarIdx
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { avatarIdx = i },
                            color = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF1E293B),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) NeonCyan else Color.Transparent
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(a, fontSize = 20.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 15) username = it },
                    label = { Text("Racer Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        focusedLabelColor = NeonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Stats Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Career Distance: ${"%.1f".format(prof.totalDistanceKm)} km", color = Color.White, fontSize = 12.sp)
                    Text("Flips Performed: ${prof.totalFlips}", color = Color.White, fontSize = 12.sp)
                    Text("Top Speed: ${prof.maxSpeedKmh.toInt()} km/h", color = Color.White, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    prefs.saveProfileUsername(username, avatarIdx)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("SAVE PROFILE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
