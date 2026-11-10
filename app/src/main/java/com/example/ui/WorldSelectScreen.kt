package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEngine
import com.example.data.GamePreferences
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun WorldSelectScreen(
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onNavigateBack: () -> Unit,
    onSelectLevel: (WorldInfo, LevelInfo) -> Unit
) {
    val totalStars = remember { prefs.getTotalStarsEarned() }
    var selectedWorldId by remember { mutableStateOf(WorldRegistry.allWorlds.first().id) }
    val currentWorld = remember(selectedWorldId) { WorldRegistry.getById(selectedWorldId) }

    Scaffold(
        containerColor = DarkNavyBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        soundEngine.playButtonClick()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CardNavy)
                        .border(1.dp, BorderCyan, CircleShape)
                        .testTag("world_select_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "WORLD SELECT",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Total Stars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardNavy)
                        .border(1.dp, BorderCyan, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("⭐ $totalStars", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // Horizontal Worlds Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(WorldRegistry.allWorlds) { w ->
                    val isSelected = w.id == selectedWorldId
                    val isUnlocked = totalStars >= w.unlockStarsRequired

                    Surface(
                        modifier = Modifier
                            .width(140.dp)
                            .height(95.dp)
                            .clickable {
                                soundEngine.playButtonClick()
                                selectedWorldId = w.id
                            }
                            .testTag("world_card_${w.id}"),
                        color = if (isSelected) Color(0xFF1E3A5F) else CardNavy,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) NeonCyan else BorderCyan
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(w.iconEmoji, fontSize = 20.sp)
                                if (!isUnlocked) {
                                    Text("🔒 ${w.unlockStarsRequired}★", color = NeonYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("✓", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = w.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // World Info Summary
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = CardNavy,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = currentWorld.name,
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = currentWorld.description,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Text(
                text = "CHOOSE LEVEL (10 ACTS)",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 10 Levels Grid
            val isCurrentWorldUnlocked = totalStars >= currentWorld.unlockStarsRequired

            if (!isCurrentWorldUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "LOCKED WORLD",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Earn ${currentWorld.unlockStarsRequired} total stars to unlock ${currentWorld.name}!",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentWorld.getLevels()) { lvl ->
                        val earnedStars = prefs.getLevelStars(lvl.id)
                        val bestDist = prefs.getBestDistance(lvl.id)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp)
                                .clickable {
                                    soundEngine.playButtonClick()
                                    onSelectLevel(currentWorld, lvl)
                                }
                                .testTag("level_card_${lvl.id}"),
                            color = CardNavy,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ACT ${lvl.levelNumber}",
                                        color = NeonCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    // Stars
                                    Row {
                                        for (s in 1..3) {
                                            Text(
                                                text = "★",
                                                fontSize = 14.sp,
                                                color = if (s <= earnedStars) NeonYellow else Color.DarkGray
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏁 ${lvl.trackLengthMeters.toInt()}m",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "🪙 +${lvl.coinReward}",
                                        color = GoldCoin,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        soundEngine.playButtonClick()
                                        onSelectLevel(currentWorld, lvl)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "PLAY",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
