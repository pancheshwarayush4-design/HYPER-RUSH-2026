package com.example.vfx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.*
import kotlin.random.Random

data class Particle(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var life: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 4f,
    var color: Color = Color.White,
    var isStarOrSparkle: Boolean = false
)

data class FloatingPopup(
    var active: Boolean = false,
    var text: String = "",
    var x: Float = 0f,
    var y: Float = 0f,
    var vy: Float = -2.5f,
    var life: Float = 0f,
    var maxLife: Float = 1.0f,
    var color: Color = GoldCoin,
    var scale: Float = 1.0f
)

data class Shockwave(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 0f,
    var maxRadius: Float = 60f,
    var life: Float = 0f,
    var maxLife: Float = 0.5f,
    var color: Color = NeonCyan
)

class ParticleSystem(
    private val maxParticles: Int = 350,
    private val maxPopups: Int = 20,
    private val maxShockwaves: Int = 10
) {
    private val particles = Array(maxParticles) { Particle() }
    private val popups = Array(maxPopups) { FloatingPopup() }
    private val shockwaves = Array(maxShockwaves) { Shockwave() }

    var cameraShakeIntensity = 0f
    var bulletTimeScale = 1.0f // 1.0 = normal, 0.4 = slow mo
    private var bulletTimeTimer = 0f

    fun triggerShake(amount: Float) {
        cameraShakeIntensity = maxOf(cameraShakeIntensity, amount).coerceAtMost(28f)
    }

    fun triggerBulletTime(duration: Float, scale: Float = 0.4f) {
        bulletTimeTimer = duration
        bulletTimeScale = scale
    }

    fun spawnFloatingPopup(text: String, x: Float, y: Float, color: Color, scale: Float = 1.0f) {
        val popup = popups.firstOrNull { !it.active } ?: return
        popup.active = true
        popup.text = text
        popup.x = x
        popup.y = y
        popup.vy = -2.8f
        popup.maxLife = 0.9f
        popup.life = popup.maxLife
        popup.color = color
        popup.scale = scale
    }

    fun spawnShockwave(x: Float, y: Float, maxRadius: Float = 70f, color: Color = NeonCyan) {
        val sw = shockwaves.firstOrNull { !it.active } ?: return
        sw.active = true
        sw.x = x
        sw.y = y
        sw.radius = 4f
        sw.maxRadius = maxRadius
        sw.maxLife = 0.45f
        sw.life = sw.maxLife
        sw.color = color
    }

    fun spawnWheelDust(x: Float, y: Float, vx: Float, surfaceColor: Color) {
        val count = 2
        for (i in 0 until count) {
            val p = acquire() ?: break
            p.active = true
            p.x = x + Random.nextFloat() * 0.4f - 0.2f
            p.y = y + Random.nextFloat() * 0.2f
            p.vx = -vx * 0.35f + (Random.nextFloat() * 4f - 2f)
            p.vy = -Random.nextFloat() * 3f - 1.2f
            p.maxLife = 0.4f + Random.nextFloat() * 0.3f
            p.life = p.maxLife
            p.size = 3f + Random.nextFloat() * 5f
            p.color = surfaceColor.copy(alpha = 0.7f)
            p.isStarOrSparkle = false
        }
    }

    fun spawnNitroFlames(x: Float, y: Float, chassisAngleRad: Float) {
        val count = 5
        val backAngle = chassisAngleRad + Math.PI.toFloat()
        val dirX = Math.cos(backAngle.toDouble()).toFloat()
        val dirY = Math.sin(backAngle.toDouble()).toFloat()

        for (i in 0 until count) {
            val p = acquire() ?: break
            p.active = true
            p.x = x + (Random.nextFloat() * 0.3f - 0.15f)
            p.y = y + (Random.nextFloat() * 0.3f - 0.15f)
            val speed = 14f + Random.nextFloat() * 10f
            p.vx = dirX * speed + (Random.nextFloat() * 4f - 2f)
            p.vy = dirY * speed + (Random.nextFloat() * 4f - 2f)
            p.maxLife = 0.28f + Random.nextFloat() * 0.2f
            p.life = p.maxLife
            p.size = 5f + Random.nextFloat() * 9f
            p.color = if (Random.nextBoolean()) NeonCyan else NitroFlame
            p.isStarOrSparkle = false
        }
    }

    fun spawnSparks(x: Float, y: Float) {
        for (i in 0 until 6) {
            val p = acquire() ?: break
            p.active = true
            p.x = x
            p.y = y
            p.vx = Random.nextFloat() * 12f - 6f
            p.vy = -Random.nextFloat() * 10f - 2f
            p.maxLife = 0.35f
            p.life = p.maxLife
            p.size = 3f + Random.nextFloat() * 3f
            p.color = NeonYellow
            p.isStarOrSparkle = true
        }
    }

    fun spawnCollectSparkles(x: Float, y: Float, isDiamond: Boolean) {
        val color = if (isDiamond) DiamondBlue else GoldCoin
        spawnShockwave(x, y, maxRadius = if (isDiamond) 55f else 40f, color = color)
        for (i in 0 until 14) {
            val p = acquire() ?: break
            p.active = true
            p.x = x
            p.y = y
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 3.5f + Random.nextFloat() * 7f
            p.vx = Math.cos(angle.toDouble()).toFloat() * speed
            p.vy = Math.sin(angle.toDouble()).toFloat() * speed
            p.maxLife = 0.65f + Random.nextFloat() * 0.4f
            p.life = p.maxLife
            p.size = 4f + Random.nextFloat() * 6f
            p.color = color
            p.isStarOrSparkle = true
        }
    }

    fun spawnLandingImpact(x: Float, y: Float, surfaceColor: Color) {
        triggerShake(7f)
        spawnShockwave(x, y, maxRadius = 60f, color = surfaceColor)
        for (i in 0 until 12) {
            val p = acquire() ?: break
            p.active = true
            p.x = x
            p.y = y
            val dir = if (Random.nextBoolean()) 1f else -1f
            p.vx = dir * (2f + Random.nextFloat() * 8f)
            p.vy = -Random.nextFloat() * 5f - 1f
            p.maxLife = 0.45f
            p.life = p.maxLife
            p.size = 4f + Random.nextFloat() * 7f
            p.color = surfaceColor.copy(alpha = 0.85f)
            p.isStarOrSparkle = false
        }
    }

    fun spawnExplosion(x: Float, y: Float) {
        triggerShake(22f)
        spawnShockwave(x, y, maxRadius = 110f, color = DangerRed)
        for (i in 0 until 40) {
            val p = acquire() ?: break
            p.active = true
            p.x = x
            p.y = y
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 5f + Random.nextFloat() * 16f
            p.vx = Math.cos(angle.toDouble()).toFloat() * speed
            p.vy = Math.sin(angle.toDouble()).toFloat() * speed
            p.maxLife = 0.75f + Random.nextFloat() * 0.5f
            p.life = p.maxLife
            p.size = 6f + Random.nextFloat() * 14f
            p.color = when (Random.nextInt(3)) {
                0 -> NitroFlame
                1 -> NeonOrange
                else -> Color(0xFF222222)
            }
            p.isStarOrSparkle = false
        }
    }

    fun spawnDebris(x: Float, y: Float, count: Int = 8, color: Color = Color.DarkGray) {
        for (i in 0 until count) {
            val p = acquire() ?: break
            p.active = true
            p.x = x + Random.nextFloat() * 0.6f - 0.3f
            p.y = y + Random.nextFloat() * 0.6f - 0.3f
            p.vx = Random.nextFloat() * 10f - 5f
            p.vy = -Random.nextFloat() * 8f - 2f
            p.maxLife = 0.6f
            p.life = p.maxLife
            p.size = 4f + Random.nextFloat() * 5f
            p.color = color
            p.isStarOrSparkle = false
        }
    }

    fun update(dt: Float, gravity: Float) {
        if (bulletTimeTimer > 0f) {
            bulletTimeTimer -= dt
            if (bulletTimeTimer <= 0f) {
                bulletTimeScale = 1.0f
            }
        }

        if (cameraShakeIntensity > 0f) {
            cameraShakeIntensity = (cameraShakeIntensity - dt * 25f).coerceAtLeast(0f)
        }

        // Update shockwaves
        for (sw in shockwaves) {
            if (!sw.active) continue
            sw.life -= dt
            if (sw.life <= 0f) {
                sw.active = false
                continue
            }
            val progress = 1f - (sw.life / sw.maxLife)
            sw.radius = progress * sw.maxRadius
        }

        // Update floating popups
        for (pop in popups) {
            if (!pop.active) continue
            pop.life -= dt
            if (pop.life <= 0f) {
                pop.active = false
                continue
            }
            pop.y += pop.vy * dt
        }

        // Update particles
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }

            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += gravity * 0.4f * dt
        }
    }

    fun draw(drawScope: DrawScope, worldToScreen: (Float, Float) -> Offset) {
        // Draw Shockwaves
        for (sw in shockwaves) {
            if (!sw.active) continue
            val center = worldToScreen(sw.x, sw.y)
            val alpha = (sw.life / sw.maxLife).coerceIn(0f, 1f)
            drawScope.drawCircle(
                color = sw.color.copy(alpha = alpha * 0.8f),
                radius = sw.radius,
                center = center,
                style = Stroke(width = 4f * alpha)
            )
        }

        // Draw Particles
        for (p in particles) {
            if (!p.active) continue
            val screenPos = worldToScreen(p.x, p.y)
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            val radius = p.size * alpha

            if (p.isStarOrSparkle) {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = radius,
                    center = screenPos
                )
            } else {
                drawScope.drawCircle(
                    color = p.color.copy(alpha = alpha * 0.85f),
                    radius = radius,
                    center = screenPos
                )
            }
        }
    }

    fun getActivePopups(): List<FloatingPopup> {
        return popups.filter { it.active }
    }

    private fun acquire(): Particle? {
        return particles.firstOrNull { !it.active }
    }
}
