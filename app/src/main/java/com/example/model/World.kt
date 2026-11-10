package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class AmbientParticleType {
    NONE,
    DUST,
    SAND_STORM,
    SNOW,
    LAVA_EMBERS,
    NEON_GLOW,
    LEAVES,
    HAUNTED_MIST,
    COSMIC_STARS,
    CANDY_SPARKLES
}

data class LevelInfo(
    val worldId: String,
    val levelNumber: Int,
    val name: String,
    val trackLengthMeters: Float,
    val hillAmplitude: Float, // height variance
    val hillFrequency: Float, // roughness
    val hasBridges: Boolean = false,
    val hasMovingPlatforms: Boolean = false,
    val hasSpikeTraps: Boolean = false,
    val hasGatePuzzle: Boolean = false,
    val coinCount: Int = 30,
    val diamondCount: Int = 2,
    val targetTimeSeconds: Float = 60f,
    val coinReward: Int = 400,
    val diamondReward: Int = 2
) {
    val id: String get() = "${worldId}_$levelNumber"
    val globalLevelIndex: Int get() {
        val worldIdx = WorldRegistry.allWorlds.indexOfFirst { it.id == worldId }.coerceAtLeast(0)
        return (worldIdx * 10) + levelNumber
    }
}

data class WorldInfo(
    val id: String,
    val name: String,
    val description: String,
    val skyGradientTop: Color,
    val skyGradientBottom: Color,
    val groundSurfaceColor: Color,
    val groundFillColor: Color,
    val rockDetailColor: Color,
    val gravity: Float = 9.8f,
    val frictionMultiplier: Float = 1.0f,
    val particleType: AmbientParticleType = AmbientParticleType.NONE,
    val unlockStarsRequired: Int = 0,
    val iconEmoji: String = "🏔️"
) {
    fun getLevels(): List<LevelInfo> {
        return (1..10).map { lvl ->
            val dist = 350f + (lvl * 85f)
            val amp = 7f + (lvl * 1.8f)
            val freq = 0.015f + (lvl * 0.003f)
            LevelInfo(
                worldId = id,
                levelNumber = lvl,
                name = "$name - Act $lvl",
                trackLengthMeters = dist,
                hillAmplitude = amp,
                hillFrequency = freq,
                hasBridges = lvl in listOf(3, 6, 8, 10),
                hasMovingPlatforms = lvl in listOf(4, 7, 9, 10),
                hasSpikeTraps = lvl in listOf(5, 8, 9, 10),
                hasGatePuzzle = lvl in listOf(4, 7, 10),
                coinCount = 20 + (lvl * 5),
                diamondCount = if (lvl % 3 == 0 || lvl == 10) 3 else 1,
                targetTimeSeconds = 30f + (lvl * 6f),
                coinReward = 300 + (lvl * 100),
                diamondReward = if (lvl == 10) 5 else 2
            )
        }
    }
}

object WorldRegistry {
    val allWorlds: List<WorldInfo> = listOf(
        WorldInfo(
            id = "green_hills",
            name = "Green Hills",
            description = "Sunny verdant meadows with sweeping rollercaster hills and scenic windmill jumps.",
            skyGradientTop = Color(0xFF1E88E5),
            skyGradientBottom = Color(0xFF90CAF9),
            groundSurfaceColor = Color(0xFF4CAF50),
            groundFillColor = Color(0xFF2E7D32),
            rockDetailColor = Color(0xFF5D4037),
            gravity = 9.8f,
            frictionMultiplier = 1.0f,
            particleType = AmbientParticleType.DUST,
            unlockStarsRequired = 0,
            iconEmoji = "🌱"
        ),
        WorldInfo(
            id = "desert_rush",
            name = "Desert Rush",
            description = "Blazing sand dunes, ancient pyramid tombs, and sudden sinkhole ramps.",
            skyGradientTop = Color(0xFFE65100),
            skyGradientBottom = Color(0xFFFFB74D),
            groundSurfaceColor = Color(0xFFFFB300),
            groundFillColor = Color(0xFFF57F17),
            rockDetailColor = Color(0xFF8D6E63),
            gravity = 9.8f,
            frictionMultiplier = 0.95f,
            particleType = AmbientParticleType.SAND_STORM,
            unlockStarsRequired = 6,
            iconEmoji = "🏜️"
        ),
        WorldInfo(
            id = "snow_mountain",
            name = "Snow Mountain",
            description = "Freezing alpine peaks with treacherous ice patches and giant glacial bridges.",
            skyGradientTop = Color(0xFF37474F),
            skyGradientBottom = Color(0xFFB0BEC5),
            groundSurfaceColor = Color(0xFFECEFF1),
            groundFillColor = Color(0xFF78909C),
            rockDetailColor = Color(0xFF455A64),
            gravity = 9.8f,
            frictionMultiplier = 0.65f, // Slippery ice!
            particleType = AmbientParticleType.SNOW,
            unlockStarsRequired = 14,
            iconEmoji = "❄️"
        ),
        WorldInfo(
            id = "volcano_escape",
            name = "Volcano Escape",
            description = "Raging magma rivers, falling embers, and explosive geysers that launch your vehicle.",
            skyGradientTop = Color(0xFF210000),
            skyGradientBottom = Color(0xFFBF360C),
            groundSurfaceColor = Color(0xFFFF3D00),
            groundFillColor = Color(0xFF3E2723),
            rockDetailColor = Color(0xFF261815),
            gravity = 10.5f,
            frictionMultiplier = 1.05f,
            particleType = AmbientParticleType.LAVA_EMBERS,
            unlockStarsRequired = 24,
            iconEmoji = "🌋"
        ),
        WorldInfo(
            id = "neon_cyber",
            name = "Neon Cyber City",
            description = "Futuristic synthwave megacity with glowing holographic tracks and magnetic rails.",
            skyGradientTop = Color(0xFF0F051D),
            skyGradientBottom = Color(0xFF3B0764),
            groundSurfaceColor = NeonCyan,
            groundFillColor = Color(0xFF1E1035),
            rockDetailColor = NeonPink,
            gravity = 9.2f,
            frictionMultiplier = 1.15f,
            particleType = AmbientParticleType.NEON_GLOW,
            unlockStarsRequired = 36,
            iconEmoji = "🏙️"
        ),
        WorldInfo(
            id = "jungle_adventure",
            name = "Jungle Adventure",
            description = "Dense ancient rainforest canopy with wobbly rope bridges and overgrown mossy ruins.",
            skyGradientTop = Color(0xFF004D40),
            skyGradientBottom = Color(0xFF80CBC4),
            groundSurfaceColor = Color(0xFF2E7D32),
            groundFillColor = Color(0xFF1B5E20),
            rockDetailColor = Color(0xFF3E2723),
            gravity = 9.8f,
            frictionMultiplier = 0.90f,
            particleType = AmbientParticleType.LEAVES,
            unlockStarsRequired = 50,
            iconEmoji = "🌴"
        ),
        WorldInfo(
            id = "haunted_forest",
            name = "Haunted Forest",
            description = "Eerie purple moonlight, gnarled dead trees, floating ghostly spirits, and cursed ravines.",
            skyGradientTop = Color(0xFF090014),
            skyGradientBottom = Color(0xFF4A148C),
            groundSurfaceColor = Color(0xFF7B1FA2),
            groundFillColor = Color(0xFF120024),
            rockDetailColor = Color(0xFF424242),
            gravity = 9.6f,
            frictionMultiplier = 0.88f,
            particleType = AmbientParticleType.HAUNTED_MIST,
            unlockStarsRequired = 65,
            iconEmoji = "🎃"
        ),
        WorldInfo(
            id = "space_galaxy",
            name = "Space Galaxy",
            description = "Low-gravity cosmic moonscape where single jumps launch into celestial orbits.",
            skyGradientTop = Color(0xFF02040A),
            skyGradientBottom = Color(0xFF1A237E),
            groundSurfaceColor = Color(0xFF8C9EFF),
            groundFillColor = Color(0xFF0D122B),
            rockDetailColor = Color(0xFF536DFE),
            gravity = 5.2f, // Low gravity floating jumps!
            frictionMultiplier = 0.80f,
            particleType = AmbientParticleType.COSMIC_STARS,
            unlockStarsRequired = 80,
            iconEmoji = "🚀"
        ),
        WorldInfo(
            id = "candy_world",
            name = "Candy World",
            description = "Bouncy jelly slopes, marshmallow speed cushions, and chocolate canyon jumps.",
            skyGradientTop = Color(0xFFFF80AB),
            skyGradientBottom = Color(0xFFFCE4EC),
            groundSurfaceColor = Color(0xFFFF4081),
            groundFillColor = Color(0xFFD81B60),
            rockDetailColor = Color(0xFF4E342E),
            gravity = 8.8f,
            frictionMultiplier = 1.05f,
            particleType = AmbientParticleType.CANDY_SPARKLES,
            unlockStarsRequired = 98,
            iconEmoji = "🍭"
        ),
        WorldInfo(
            id = "impossible_island",
            name = "Impossible Island",
            description = "Extreme floating sky islands linked by precarious moving cranes and death-defying loop gaps.",
            skyGradientTop = Color(0xFF1A1A2E),
            skyGradientBottom = Color(0xFF16213E),
            groundSurfaceColor = Color(0xFF00E5FF),
            groundFillColor = Color(0xFF0F3460),
            rockDetailColor = Color(0xFFE94560),
            gravity = 10.2f,
            frictionMultiplier = 1.10f,
            particleType = AmbientParticleType.NEON_GLOW,
            unlockStarsRequired = 115,
            iconEmoji = "⚡"
        )
    )

    fun getById(id: String): WorldInfo {
        return allWorlds.find { it.id == id } ?: allWorlds[0]
    }
}
