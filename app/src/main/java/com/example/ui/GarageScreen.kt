package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun GarageScreen(
    prefs: GamePreferences,
    soundEngine: SoundEngine,
    onNavigateBack: () -> Unit,
    onStartRace: () -> Unit
) {
    val coins by prefs.coinsFlow.collectAsState()
    val diamonds by prefs.diamondsFlow.collectAsState()

    var selectedVehicleId by remember { mutableStateOf(prefs.getSelectedVehicleId()) }
    val currentVehicle = remember(selectedVehicleId) { VehicleRegistry.getById(selectedVehicleId) }

    val isUnlocked = remember(selectedVehicleId, coins, diamonds) {
        prefs.isVehicleUnlocked(selectedVehicleId)
    }

    var selectedSkin by remember(selectedVehicleId) {
        mutableStateOf(prefs.getVehicleSkin(selectedVehicleId))
    }
    var selectedUnderglow by remember(selectedVehicleId) {
        mutableStateOf(prefs.getVehicleUnderglow(selectedVehicleId))
    }

    // Refresh trigger for upgrades
    var upgradeRefresh by remember { mutableIntStateOf(0) }

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
                        .testTag("garage_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "GARAGE",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Currency
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardNavy)
                        .border(1.dp, BorderCyan, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🪙 $coins", color = GoldCoin, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("💎 $diamonds", color = DiamondBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        bottomBar = {
            Surface(
                color = CardNavy,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUnlocked) {
                        Button(
                            onClick = {
                                soundEngine.playButtonClick()
                                prefs.setSelectedVehicleId(selectedVehicleId)
                                onStartRace()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("race_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "RACE WITH THIS VEHICLE ➔",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentVehicle.unlockDiamonds > 0) {
                                    if (prefs.spendDiamonds(currentVehicle.unlockDiamonds)) {
                                        prefs.unlockVehicle(selectedVehicleId)
                                        prefs.setSelectedVehicleId(selectedVehicleId)
                                        soundEngine.playStuntFanfare()
                                        upgradeRefresh++
                                    }
                                } else {
                                    if (prefs.spendCoins(currentVehicle.unlockCoins)) {
                                        prefs.unlockVehicle(selectedVehicleId)
                                        prefs.setSelectedVehicleId(selectedVehicleId)
                                        soundEngine.playStuntFanfare()
                                        upgradeRefresh++
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("unlock_vehicle_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            val costText = if (currentVehicle.unlockDiamonds > 0) {
                                "UNLOCK FOR 💎 ${currentVehicle.unlockDiamonds}"
                            } else {
                                "UNLOCK FOR 🪙 ${currentVehicle.unlockCoins}"
                            }
                            Text(
                                text = costText,
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // Horizontal Vehicle Selector Strip
            Text(
                text = "SELECT VEHICLE (${VehicleRegistry.allVehicles.indexOfFirst { it.id == selectedVehicleId } + 1} of 12)",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(VehicleRegistry.allVehicles) { veh ->
                    val isSelected = veh.id == selectedVehicleId
                    val isVehUnlocked = prefs.isVehicleUnlocked(veh.id)

                    Surface(
                        modifier = Modifier
                            .width(130.dp)
                            .height(85.dp)
                            .clickable {
                                soundEngine.playButtonClick()
                                selectedVehicleId = veh.id
                                prefs.setSelectedVehicleId(veh.id)
                            }
                            .testTag("vehicle_chip_${veh.id}"),
                        color = if (isSelected) Color(0xFF1E3A5F) else CardNavy,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) NeonCyan else BorderCyan
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isVehUnlocked) "✓" else "🔒",
                                    fontSize = 12.sp,
                                    color = if (isVehUnlocked) NeonGreen else Color.LightGray
                                )
                                if (isSelected) {
                                    Text("ACTIVE", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = veh.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vehicle Inspection Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = CardNavy,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentVehicle.name,
                        color = NeonCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = currentVehicle.description,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    // Vehicle Attribute Bars
                    VehicleStatBar(label = "Top Speed", valueRatio = (currentVehicle.baseMaxSpeed / 45f))
                    VehicleStatBar(label = "Acceleration", valueRatio = (currentVehicle.baseEnginePower / 260f))
                    VehicleStatBar(label = "Suspension Travel", valueRatio = (currentVehicle.baseSuspensionStiffness / 65f))
                    VehicleStatBar(label = "Fuel Capacity", valueRatio = (currentVehicle.baseFuelTank / 70f))
                    VehicleStatBar(label = "Nitro Power", valueRatio = (currentVehicle.baseNitroCapacity / 10f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UPGRADE WORKSHOP
            if (isUnlocked) {
                Text(
                    text = "UPGRADE WORKSHOP (MAX LVL 10)",
                    color = NeonOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UpgradeItemCard(
                        title = "Engine",
                        icon = "⚡",
                        description = "+12% Acceleration & Max Velocity",
                        currentLevel = prefs.getUpgradeLevel(selectedVehicleId, "engine"),
                        onUpgrade = {
                            val cur = prefs.getUpgradeLevel(selectedVehicleId, "engine")
                            val cost = (cur + 1) * 350
                            if (prefs.spendCoins(cost)) {
                                prefs.setUpgradeLevel(selectedVehicleId, "engine", cur + 1)
                                soundEngine.playFuel()
                                upgradeRefresh++
                            }
                        }
                    )

                    UpgradeItemCard(
                        title = "Tires & Grip",
                        icon = "🛞",
                        description = "+10% Hill Traction & Brake Grip",
                        currentLevel = prefs.getUpgradeLevel(selectedVehicleId, "tires"),
                        onUpgrade = {
                            val cur = prefs.getUpgradeLevel(selectedVehicleId, "tires")
                            val cost = (cur + 1) * 350
                            if (prefs.spendCoins(cost)) {
                                prefs.setUpgradeLevel(selectedVehicleId, "tires", cur + 1)
                                soundEngine.playFuel()
                                upgradeRefresh++
                            }
                        }
                    )

                    UpgradeItemCard(
                        title = "Suspension",
                        icon = "⚙️",
                        description = "+12% Impact Absorption & Cushion",
                        currentLevel = prefs.getUpgradeLevel(selectedVehicleId, "suspension"),
                        onUpgrade = {
                            val cur = prefs.getUpgradeLevel(selectedVehicleId, "suspension")
                            val cost = (cur + 1) * 350
                            if (prefs.spendCoins(cost)) {
                                prefs.setUpgradeLevel(selectedVehicleId, "suspension", cur + 1)
                                soundEngine.playFuel()
                                upgradeRefresh++
                            }
                        }
                    )

                    UpgradeItemCard(
                        title = "Fuel Cell",
                        icon = "⛽",
                        description = "+15% Longer Driving Range",
                        currentLevel = prefs.getUpgradeLevel(selectedVehicleId, "fuel"),
                        onUpgrade = {
                            val cur = prefs.getUpgradeLevel(selectedVehicleId, "fuel")
                            val cost = (cur + 1) * 350
                            if (prefs.spendCoins(cost)) {
                                prefs.setUpgradeLevel(selectedVehicleId, "fuel", cur + 1)
                                soundEngine.playFuel()
                                upgradeRefresh++
                            }
                        }
                    )

                    UpgradeItemCard(
                        title = "Nitro Boost",
                        icon = "🔥",
                        description = "+15% Boost Capacity & Thrust",
                        currentLevel = prefs.getUpgradeLevel(selectedVehicleId, "nitro"),
                        onUpgrade = {
                            val cur = prefs.getUpgradeLevel(selectedVehicleId, "nitro")
                            val cost = (cur + 1) * 350
                            if (prefs.spendCoins(cost)) {
                                prefs.setUpgradeLevel(selectedVehicleId, "nitro", cur + 1)
                                soundEngine.playFuel()
                                upgradeRefresh++
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CUSTOMIZATION: SKINS & UNDERGLOW
                Text(
                    text = "CUSTOM SKINS & NEON UNDERGLOW",
                    color = NeonPink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = CardNavy,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Paint Skin:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VehicleSkin.values().forEach { skin ->
                                val isSkinSelected = selectedSkin == skin
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(skin.primaryColor)
                                        .border(
                                            if (isSkinSelected) 3.dp else 1.dp,
                                            if (isSkinSelected) Color.White else Color.DarkGray,
                                            CircleShape
                                        )
                                        .clickable {
                                            selectedSkin = skin
                                            prefs.setVehicleSkin(selectedVehicleId, skin)
                                            soundEngine.playButtonClick()
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Neon Underglow:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NeonUnderglow.values().forEach { glow ->
                                val isGlowSelected = selectedUnderglow == glow
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(glow.color ?: Color.Black)
                                        .border(
                                            if (isGlowSelected) 3.dp else 1.dp,
                                            if (isGlowSelected) Color.White else Color.DarkGray,
                                            CircleShape
                                        )
                                        .clickable {
                                            selectedUnderglow = glow
                                            prefs.setVehicleUnderglow(selectedVehicleId, glow)
                                            soundEngine.playButtonClick()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (glow == NeonUnderglow.NONE) {
                                        Text("✕", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleStatBar(label: String, valueRatio: Float) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.LightGray, fontSize = 12.sp)
            Text(text = "${(valueRatio * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { valueRatio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = NeonCyan,
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun UpgradeItemCard(
    title: String,
    icon: String,
    description: String,
    currentLevel: Int,
    onUpgrade: () -> Unit
) {
    val isMax = currentLevel >= 10
    val cost = (currentLevel + 1) * 350

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardNavy,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMax) "MAX" else "LVL $currentLevel/10",
                            color = if (isMax) NeonYellow else NeonCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                    Text(text = description, color = Color.LightGray, fontSize = 11.sp)
                }
            }

            if (!isMax) {
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("upgrade_${title.lowercase()}_button")
                ) {
                    Text("🪙 $cost", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Text("MAXED", color = NeonYellow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}
