package com.example.model

import kotlin.math.cos
import kotlin.math.sin

data class Gate(
    val id: String,
    val x: Float, // world meters
    val y: Float,
    val width: Float = 1.2f,
    val height: Float = 6.0f,
    var isOpen: Boolean = false,
    val switchId: String? = null,
    val requiresKey: Boolean = false,
    var openProgress: Float = 0f // 0f = closed, 1f = fully opened
)

data class PressureSwitch(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float = 1.0f,
    var isPressed: Boolean = false,
    var pressAnim: Float = 0f
)

data class KeyPickup(
    val id: String,
    val x: Float,
    val y: Float,
    val targetGateId: String,
    var isCollected: Boolean = false
)

data class MovingPlatform(
    val id: String,
    val startX: Float,
    val endX: Float,
    val startY: Float,
    val endY: Float,
    val width: Float = 5.0f,
    val height: Float = 0.8f,
    val speed: Float = 2.0f, // m/s
    var currentX: Float = startX,
    var currentY: Float = startY,
    var forward: Boolean = true
) {
    fun update(dt: Float) {
        val dx = endX - startX
        val dy = endY - startY
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist <= 0.001f) return
        val step = (speed * dt) / dist
        if (forward) {
            val progress = ((currentX - startX) / (if (dx != 0f) dx else 1f)) + step
            if (progress >= 1f) {
                currentX = endX
                currentY = endY
                forward = false
            } else {
                currentX = startX + dx * progress
                currentY = startY + dy * progress
            }
        } else {
            val progress = ((currentX - startX) / (if (dx != 0f) dx else 1f)) - step
            if (progress <= 0f) {
                currentX = startX
                currentY = startY
                forward = true
            } else {
                currentX = startX + dx * progress
                currentY = startY + dy * progress
            }
        }
    }
}

data class SpikeTrap(
    val x: Float,
    val y: Float,
    val width: Float = 4.0f,
    val height: Float = 1.2f
)

data class RopeBridgeSegment(
    var x: Float,
    var y: Float,
    val targetY: Float,
    var vy: Float = 0f
)

data class RopeBridge(
    val startX: Float,
    val endX: Float,
    val y: Float,
    val segments: List<RopeBridgeSegment>
)

data class StarPickup(
    val index: Int, // 1, 2, or 3
    val x: Float,
    val y: Float,
    var isCollected: Boolean = false
)

data class CollectibleItem(
    val id: String,
    var x: Float,
    var y: Float,
    val type: CollectibleType,
    val value: Int = 10,
    var isCollected: Boolean = false,
    var collectAnimProgress: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isAttracted: Boolean = false
)

enum class CollectibleType {
    COIN,
    DIAMOND,
    FUEL,
    NITRO,
    MAGNET,
    MYSTERY_CHEST
}

// 20 NEW CHALLENGING OBSTACLES & PUZZLES

data class SpeedBoostPad(
    val x: Float,
    val y: Float,
    val width: Float = 4.0f,
    val boostSpeed: Float = 35f
)

data class LaunchSpring(
    val x: Float,
    val y: Float,
    val width: Float = 3.0f,
    val impulseVy: Float = -22f, // upward leap
    var isTriggered: Boolean = false,
    var bounceAnim: Float = 0f
)

data class SeesawBridge(
    val pivotX: Float,
    val pivotY: Float,
    val length: Float = 16.0f,
    var angleRad: Float = 0f,
    var angularVelocity: Float = 0f
)

data class CollapsingBridge(
    val startX: Float,
    val endX: Float,
    val y: Float,
    var isSteppedOn: Boolean = false,
    var collapseTimer: Float = 0f,
    var isFallen: Boolean = false,
    var fallOffsetY: Float = 0f,
    var fallVy: Float = 0f
)

data class RollingBoulder(
    var x: Float,
    var y: Float,
    var vx: Float = -4f,
    var vy: Float = 0f,
    val radius: Float = 1.8f,
    var rotationAngle: Float = 0f
)

data class LaserGrid(
    val x: Float,
    val y: Float,
    val height: Float = 6.0f,
    val cycleDuration: Float = 3.0f,
    var timer: Float = 0f
) {
    val isDangerous: Boolean get() = (timer % cycleDuration) < (cycleDuration * 0.55f)
}

data class PendulumBlade(
    val pivotX: Float,
    val pivotY: Float,
    val length: Float = 8.0f,
    val bladeRadius: Float = 1.4f,
    val period: Float = 2.4f,
    var timer: Float = 0f
) {
    val currentAngleRad: Float get() = (sin(timer * (2f * Math.PI.toFloat() / period)) * 0.9f)
    val bladeX: Float get() = pivotX + (sin(currentAngleRad) * length)
    val bladeY: Float get() = pivotY + (cos(currentAngleRad) * length)
}

data class WindTunnel(
    val startX: Float,
    val endX: Float,
    val startY: Float,
    val endY: Float,
    val pushVx: Float = 15f,
    val pushVy: Float = -12f
)

data class ExplosiveBarrel(
    val x: Float,
    val y: Float,
    val radius: Float = 1.0f,
    var isExploded: Boolean = false
)

data class SecretPath(
    val id: String,
    val startX: Float,
    val endX: Float,
    val points: List<Pair<Float, Float>>,
    var isDiscovered: Boolean = false
)

data class PushableCrate(
    val id: String,
    var x: Float,
    var y: Float,
    val size: Float = 1.6f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isGrounded: Boolean = false
)

data class MudPit(
    val x: Float,
    val y: Float,
    val width: Float = 12.0f,
    val dragFactor: Float = 0.45f
)

data class IcePatch(
    val x: Float,
    val y: Float,
    val width: Float = 14.0f
)

data class ConveyorBelt(
    val x: Float,
    val y: Float,
    val width: Float = 10.0f,
    val beltSpeed: Float = 6.0f // positive moves forward, negative pushes back
)

data class BreakablePillar(
    val x: Float,
    val y: Float,
    val width: Float = 1.8f,
    val height: Float = 5.0f,
    var isBroken: Boolean = false
)

data class BouncyMushroom(
    val x: Float,
    val y: Float,
    val width: Float = 3.5f,
    val bounceImpulse: Float = -18f,
    var bounceAnim: Float = 0f
)

data class FallingStalactite(
    val x: Float,
    val triggerDistance: Float = 10f,
    var currentY: Float,
    val groundY: Float,
    var isTriggered: Boolean = false,
    var vy: Float = 0f
)

data class AntiGravityZone(
    val startX: Float,
    val endX: Float,
    val y: Float,
    val height: Float = 10f
)

data class MysteryChest(
    val x: Float,
    val y: Float,
    val coinsReward: Int = 500,
    val diamondsReward: Int = 3,
    var isOpened: Boolean = false
)
