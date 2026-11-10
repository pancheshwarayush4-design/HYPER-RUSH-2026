package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.NeonUnderglow
import com.example.model.VehicleSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerProfile(
    val username: String = "ApexRacer",
    val avatarIndex: Int = 0,
    val totalDistanceKm: Float = 0f,
    val totalFlips: Int = 0,
    val maxSpeedKmh: Float = 0f,
    val totalCoinsEarned: Long = 0L
)

data class DailyMission(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val current: Int = 0,
    val coinReward: Int,
    val diamondReward: Int,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean get() = current >= target
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val current: Int,
    val diamondReward: Int,
    val isClaimed: Boolean
) {
    val isCompleted: Boolean get() = current >= target
}

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hyper_rush_save_data", Context.MODE_PRIVATE)

    private val _coinsFlow = MutableStateFlow(getCoins())
    val coinsFlow: StateFlow<Int> = _coinsFlow.asStateFlow()

    private val _diamondsFlow = MutableStateFlow(getDiamonds())
    val diamondsFlow: StateFlow<Int> = _diamondsFlow.asStateFlow()

    fun getCoins(): Int = prefs.getInt("coins", 1000)
    fun addCoins(amount: Int) {
        val updated = (getCoins() + amount).coerceAtLeast(0)
        prefs.edit().putInt("coins", updated).apply()
        _coinsFlow.value = updated
        addStatTotalCoins(amount.toLong())
    }

    fun spendCoins(amount: Int): Boolean {
        val cur = getCoins()
        if (cur >= amount) {
            val updated = cur - amount
            prefs.edit().putInt("coins", updated).apply()
            _coinsFlow.value = updated
            return true
        }
        return false
    }

    fun getDiamonds(): Int = prefs.getInt("diamonds", 15)
    fun addDiamonds(amount: Int) {
        val updated = (getDiamonds() + amount).coerceAtLeast(0)
        prefs.edit().putInt("diamonds", updated).apply()
        _diamondsFlow.value = updated
    }

    fun spendDiamonds(amount: Int): Boolean {
        val cur = getDiamonds()
        if (cur >= amount) {
            val updated = cur - amount
            prefs.edit().putInt("diamonds", updated).apply()
            _diamondsFlow.value = updated
            return true
        }
        return false
    }

    fun getSelectedVehicleId(): String = prefs.getString("selected_vehicle", "jeep") ?: "jeep"
    fun setSelectedVehicleId(id: String) {
        prefs.edit().putString("selected_vehicle", id).apply()
    }

    fun isVehicleUnlocked(id: String): Boolean {
        if (id == "jeep") return true
        val set = prefs.getStringSet("unlocked_vehicles", setOf("jeep")) ?: setOf("jeep")
        return set.contains(id)
    }

    fun unlockVehicle(id: String) {
        val set = prefs.getStringSet("unlocked_vehicles", setOf("jeep"))?.toMutableSet() ?: mutableSetOf("jeep")
        set.add(id)
        prefs.edit().putStringSet("unlocked_vehicles", set).apply()
    }

    fun getUpgradeLevel(vehicleId: String, part: String): Int {
        return prefs.getInt("upgrade_${vehicleId}_$part", 0)
    }

    fun setUpgradeLevel(vehicleId: String, part: String, level: Int) {
        prefs.edit().putInt("upgrade_${vehicleId}_$part", level).apply()
    }

    fun getVehicleSkin(vehicleId: String): VehicleSkin {
        val name = prefs.getString("skin_$vehicleId", VehicleSkin.STANDARD.name) ?: VehicleSkin.STANDARD.name
        return runCatching { VehicleSkin.valueOf(name) }.getOrDefault(VehicleSkin.STANDARD)
    }

    fun setVehicleSkin(vehicleId: String, skin: VehicleSkin) {
        prefs.edit().putString("skin_$vehicleId", skin.name).apply()
    }

    fun getVehicleUnderglow(vehicleId: String): NeonUnderglow {
        val name = prefs.getString("underglow_$vehicleId", NeonUnderglow.CYAN.name) ?: NeonUnderglow.CYAN.name
        return runCatching { NeonUnderglow.valueOf(name) }.getOrDefault(NeonUnderglow.CYAN)
    }

    fun setVehicleUnderglow(vehicleId: String, underglow: NeonUnderglow) {
        prefs.edit().putString("underglow_$vehicleId", underglow.name).apply()
    }

    fun getLevelStars(levelId: String): Int {
        return prefs.getInt("stars_$levelId", 0)
    }

    fun setLevelStars(levelId: String, stars: Int) {
        val current = getLevelStars(levelId)
        if (stars > current) {
            prefs.edit().putInt("stars_$levelId", stars).apply()
        }
    }

    fun getTotalStarsEarned(): Int {
        val all = prefs.all
        var count = 0
        for ((k, v) in all) {
            if (k.startsWith("stars_") && v is Int) {
                count += v
            }
        }
        return count
    }

    fun getBestDistance(levelId: String): Float = prefs.getFloat("best_dist_$levelId", 0f)
    fun setBestDistance(levelId: String, distance: Float) {
        val cur = getBestDistance(levelId)
        if (distance > cur) {
            prefs.edit().putFloat("best_dist_$levelId", distance).apply()
        }
    }

    // Audio and Settings
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) = prefs.edit().putBoolean("music_enabled", value).apply()

    var volume: Float
        get() = prefs.getFloat("volume", 0.85f)
        set(value) = prefs.edit().putFloat("volume", value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    var controlSensitivity: Float
        get() = prefs.getFloat("sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("sensitivity", value).apply()

    // Player Profile
    fun getProfile(): PlayerProfile {
        return PlayerProfile(
            username = prefs.getString("username", "ApexRacer") ?: "ApexRacer",
            avatarIndex = prefs.getInt("avatar_idx", 0),
            totalDistanceKm = prefs.getFloat("stat_dist", 0f),
            totalFlips = prefs.getInt("stat_flips", 0),
            maxSpeedKmh = prefs.getFloat("stat_speed", 0f),
            totalCoinsEarned = prefs.getLong("stat_coins", 0L)
        )
    }

    fun saveProfileUsername(name: String, avatar: Int) {
        prefs.edit().putString("username", name).putInt("avatar_idx", avatar).apply()
    }

    fun recordGameStats(distMeters: Float, flips: Int, maxSpeed: Float) {
        val curDist = prefs.getFloat("stat_dist", 0f) + (distMeters / 1000f)
        val curFlips = prefs.getInt("stat_flips", 0) + flips
        val curSpeed = maxOf(prefs.getFloat("stat_speed", 0f), maxSpeed)
        prefs.edit()
            .putFloat("stat_dist", curDist)
            .putInt("stat_flips", curFlips)
            .putFloat("stat_speed", curSpeed)
            .apply()
    }

    private fun addStatTotalCoins(amount: Long) {
        val current = prefs.getLong("stat_coins", 0L)
        prefs.edit().putLong("stat_coins", current + amount).apply()
    }

    // Daily Rewards
    fun getDailyStreak(): Int = prefs.getInt("daily_streak", 1)
    fun getLastDailyClaimDay(): Long = prefs.getLong("last_daily_claim_day", 0L)

    fun claimDailyReward(): Int {
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val lastDay = getLastDailyClaimDay()
        var streak = getDailyStreak()

        if (currentDay - lastDay == 1L) {
            streak = if (streak >= 7) 1 else streak + 1
        } else if (currentDay - lastDay > 1L) {
            streak = 1
        }

        prefs.edit()
            .putInt("daily_streak", streak)
            .putLong("last_daily_claim_day", currentDay)
            .apply()

        // Reward scaling
        val reward = streak * 300
        addCoins(reward)
        if (streak % 3 == 0) addDiamonds(3)
        return reward
    }

    fun canClaimDailyReward(): Boolean {
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val lastDay = getLastDailyClaimDay()
        return currentDay > lastDay
    }
}
