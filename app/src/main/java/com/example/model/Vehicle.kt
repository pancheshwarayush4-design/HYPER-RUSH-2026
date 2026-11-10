package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

data class VehicleUpgrade(
    val level: Int = 0,
    val maxLevel: Int = 10
) {
    val multiplier: Float get() = 1f + (level * 0.12f)
    val upgradeCost: Int get() = (level + 1) * 350
}

enum class VehicleSkin(val displayName: String, val primaryColor: Color, val accentColor: Color) {
    STANDARD("Standard", Color(0xFF2563EB), NeonCyan),
    CYBER_NEON("Cyber Neon", Color(0xFF0F172A), NeonCyan),
    CRIMSON_FURY("Crimson Fury", Color(0xFFDC2626), NeonOrange),
    GOLDEN_SUN("Golden Glory", Color(0xFFD97706), NeonYellow),
    STEALTH_BLACK("Stealth Carbon", Color(0xFF18181B), NeonPurple)
}

enum class NeonUnderglow(val displayName: String, val color: Color?) {
    NONE("None", null),
    CYAN("Pulse Cyan", NeonCyan),
    MAGENTA("Neon Pink", NeonPink),
    LIME("Acid Lime", NeonGreen),
    PURPLE("Void Purple", NeonPurple),
    GOLD("Hyper Gold", NeonYellow)
}

data class VehicleType(
    val id: String,
    val name: String,
    val description: String,
    val baseMass: Float, // kg
    val baseEnginePower: Float, // horsepower/torque factor
    val baseMaxSpeed: Float, // m/s
    val baseSuspensionStiffness: Float, // spring k
    val baseSuspensionDamping: Float, // damping c
    val baseTireFriction: Float, // grip
    val baseFuelTank: Float, // seconds of fuel
    val baseNitroCapacity: Float, // seconds of boost
    val wheelRadius: Float, // visual & physics radius in meters
    val wheelbase: Float, // distance between front and rear axle
    val chassisWidth: Float,
    val chassisHeight: Float,
    val unlockCoins: Int,
    val unlockDiamonds: Int,
    val isTwoWheeler: Boolean = false,
    val hasTracks: Boolean = false,
    val isHover: Boolean = false
)

object VehicleRegistry {
    val allVehicles: List<VehicleType> = listOf(
        VehicleType(
            id = "jeep",
            name = "Classic Jeep",
            description = "Agile, balanced 4x4 with great suspension travel for all-terrain adventures.",
            baseMass = 950f,
            baseEnginePower = 110f,
            baseMaxSpeed = 24f,
            baseSuspensionStiffness = 38f,
            baseSuspensionDamping = 4.2f,
            baseTireFriction = 1.25f,
            baseFuelTank = 45f,
            baseNitroCapacity = 5f,
            wheelRadius = 0.52f,
            wheelbase = 2.2f,
            chassisWidth = 2.8f,
            chassisHeight = 1.3f,
            unlockCoins = 0,
            unlockDiamonds = 0
        ),
        VehicleType(
            id = "monster_truck",
            name = "Monster Truck",
            description = "Giant tires and extreme spring travel to crush rugged rocks and deep craters.",
            baseMass = 1600f,
            baseEnginePower = 175f,
            baseMaxSpeed = 22f,
            baseSuspensionStiffness = 55f,
            baseSuspensionDamping = 5.5f,
            baseTireFriction = 1.45f,
            baseFuelTank = 50f,
            baseNitroCapacity = 6f,
            wheelRadius = 0.82f,
            wheelbase = 2.6f,
            chassisWidth = 3.2f,
            chassisHeight = 1.6f,
            unlockCoins = 2500,
            unlockDiamonds = 0
        ),
        VehicleType(
            id = "sports_car",
            name = "Sports Car",
            description = "Low center of gravity and aerodynamic downforce for high-speed hill launches.",
            baseMass = 880f,
            baseEnginePower = 160f,
            baseMaxSpeed = 32f,
            baseSuspensionStiffness = 32f,
            baseSuspensionDamping = 3.8f,
            baseTireFriction = 1.35f,
            baseFuelTank = 40f,
            baseNitroCapacity = 7f,
            wheelRadius = 0.42f,
            wheelbase = 2.4f,
            chassisWidth = 3.0f,
            chassisHeight = 0.95f,
            unlockCoins = 5000,
            unlockDiamonds = 10
        ),
        VehicleType(
            id = "superbike",
            name = "Superbike",
            description = "Ultra-lightweight two-wheel beast that flips with lightning speed.",
            baseMass = 320f,
            baseEnginePower = 130f,
            baseMaxSpeed = 30f,
            baseSuspensionStiffness = 28f,
            baseSuspensionDamping = 3.2f,
            baseTireFriction = 1.20f,
            baseFuelTank = 32f,
            baseNitroCapacity = 5f,
            wheelRadius = 0.44f,
            wheelbase = 1.6f,
            chassisWidth = 1.9f,
            chassisHeight = 1.0f,
            unlockCoins = 7500,
            unlockDiamonds = 15,
            isTwoWheeler = true
        ),
        VehicleType(
            id = "tractor",
            name = "Hill Tractor",
            description = "Huge rear wheels and gargantuan low-end torque. Can conquer 75° inclines.",
            baseMass = 1700f,
            baseEnginePower = 180f,
            baseMaxSpeed = 18f,
            baseSuspensionStiffness = 45f,
            baseSuspensionDamping = 4.8f,
            baseTireFriction = 1.65f,
            baseFuelTank = 60f,
            baseNitroCapacity = 4f,
            wheelRadius = 0.75f,
            wheelbase = 2.1f,
            chassisWidth = 2.6f,
            chassisHeight = 1.7f,
            unlockCoins = 10000,
            unlockDiamonds = 20
        ),
        VehicleType(
            id = "offroad_suv",
            name = "Offroad SUV",
            description = "Armored luxury chassis equipped with dual winch suspension and extended fuel cell.",
            baseMass = 1250f,
            baseEnginePower = 140f,
            baseMaxSpeed = 26f,
            baseSuspensionStiffness = 40f,
            baseSuspensionDamping = 4.5f,
            baseTireFriction = 1.30f,
            baseFuelTank = 55f,
            baseNitroCapacity = 6f,
            wheelRadius = 0.54f,
            wheelbase = 2.5f,
            chassisWidth = 3.1f,
            chassisHeight = 1.35f,
            unlockCoins = 14000,
            unlockDiamonds = 25
        ),
        VehicleType(
            id = "racing_buggy",
            name = "Racing Buggy",
            description = "Open tubular steel rollcage designed for huge airtime and aerial stunts.",
            baseMass = 720f,
            baseEnginePower = 145f,
            baseMaxSpeed = 28f,
            baseSuspensionStiffness = 35f,
            baseSuspensionDamping = 3.9f,
            baseTireFriction = 1.38f,
            baseFuelTank = 42f,
            baseNitroCapacity = 6.5f,
            wheelRadius = 0.48f,
            wheelbase = 2.2f,
            chassisWidth = 2.7f,
            chassisHeight = 1.15f,
            unlockCoins = 18000,
            unlockDiamonds = 35
        ),
        VehicleType(
            id = "formula_car",
            name = "Formula One",
            description = "High aerodynamic downforce ground-hugger that reaches ridiculous velocities.",
            baseMass = 640f,
            baseEnginePower = 190f,
            baseMaxSpeed = 38f,
            baseSuspensionStiffness = 26f,
            baseSuspensionDamping = 3.4f,
            baseTireFriction = 1.50f,
            baseFuelTank = 35f,
            baseNitroCapacity = 8f,
            wheelRadius = 0.40f,
            wheelbase = 2.7f,
            chassisWidth = 3.4f,
            chassisHeight = 0.85f,
            unlockCoins = 24000,
            unlockDiamonds = 45
        ),
        VehicleType(
            id = "snowmobile",
            name = "Arctic Snowmobile",
            description = "Front steer skis and rear rubber treads. Glides across ice, snow, and ramps effortlessly.",
            baseMass = 580f,
            baseEnginePower = 135f,
            baseMaxSpeed = 29f,
            baseSuspensionStiffness = 33f,
            baseSuspensionDamping = 3.6f,
            baseTireFriction = 1.28f,
            baseFuelTank = 44f,
            baseNitroCapacity = 6f,
            wheelRadius = 0.45f,
            wheelbase = 2.0f,
            chassisWidth = 2.6f,
            chassisHeight = 1.1f,
            unlockCoins = 30000,
            unlockDiamonds = 60,
            hasTracks = true
        ),
        VehicleType(
            id = "hover_car",
            name = "Quantum Hover",
            description = "Frictionless magnetic repulsor pads that float smoothly above rocky terrain.",
            baseMass = 800f,
            baseEnginePower = 170f,
            baseMaxSpeed = 34f,
            baseSuspensionStiffness = 30f,
            baseSuspensionDamping = 3.0f,
            baseTireFriction = 1.15f,
            baseFuelTank = 48f,
            baseNitroCapacity = 7.5f,
            wheelRadius = 0.38f,
            wheelbase = 2.5f,
            chassisWidth = 3.1f,
            chassisHeight = 1.0f,
            unlockCoins = 40000,
            unlockDiamonds = 80,
            isHover = true
        ),
        VehicleType(
            id = "armored_truck",
            name = "Titan Armored",
            description = "Heavy reinforced steel ram that smashes through obstacles without shaking.",
            baseMass = 2400f,
            baseEnginePower = 230f,
            baseMaxSpeed = 23f,
            baseSuspensionStiffness = 65f,
            baseSuspensionDamping = 6.2f,
            baseTireFriction = 1.55f,
            baseFuelTank = 70f,
            baseNitroCapacity = 5f,
            wheelRadius = 0.68f,
            wheelbase = 3.0f,
            chassisWidth = 3.6f,
            chassisHeight = 1.8f,
            unlockCoins = 55000,
            unlockDiamonds = 100
        ),
        VehicleType(
            id = "hypercar_2026",
            name = "Hyper Rush 2026",
            description = "The ultimate pinnacle of 2026 racing engineering. Twin jet thrusters and carbon nanotech.",
            baseMass = 780f,
            baseEnginePower = 260f,
            baseMaxSpeed = 44f,
            baseSuspensionStiffness = 36f,
            baseSuspensionDamping = 4.0f,
            baseTireFriction = 1.70f,
            baseFuelTank = 55f,
            baseNitroCapacity = 10f,
            wheelRadius = 0.46f,
            wheelbase = 2.8f,
            chassisWidth = 3.3f,
            chassisHeight = 0.9f,
            unlockCoins = 75000,
            unlockDiamonds = 150
        )
    )

    fun getById(id: String): VehicleType {
        return allVehicles.find { it.id == id } ?: allVehicles[0]
    }
}
