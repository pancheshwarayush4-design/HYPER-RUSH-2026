package com.example.physics

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundEngine
import com.example.data.GamePreferences
import com.example.model.*
import com.example.ui.theme.*
import com.example.vfx.ParticleSystem
import kotlin.math.*

data class Wheel(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angularVelocity: Float = 0f,
    var rotationAngle: Float = 0f,
    var isGrounded: Boolean = false,
    var suspensionCompression: Float = 0f
)

class PhysicsEngine(
    val vehicleType: VehicleType,
    val upgrades: Map<String, Int>,
    val world: WorldInfo,
    val level: GeneratedLevel,
    private val soundEngine: SoundEngine,
    private val particleSystem: ParticleSystem,
    private val prefs: GamePreferences
) {
    var x: Float = 0f
    var y: Float = 14f
    var vx: Float = 0f
    var vy: Float = 0f
    var angleRad: Float = 0f
    var angularVelocity: Float = 0f

    val rearWheel = Wheel()
    val frontWheel = Wheel()

    val engineMultiplier: Float = 1f + (upgrades["engine"] ?: 0) * 0.12f
    val tiresMultiplier: Float = 1f + (upgrades["tires"] ?: 0) * 0.10f
    val suspensionMultiplier: Float = 1f + (upgrades["suspension"] ?: 0) * 0.12f
    val fuelTankMultiplier: Float = 1f + (upgrades["fuel"] ?: 0) * 0.15f
    val nitroMultiplier: Float = 1f + (upgrades["nitro"] ?: 0) * 0.15f

    val maxFuelSeconds: Float = vehicleType.baseFuelTank * fuelTankMultiplier
    var currentFuel: Float = maxFuelSeconds

    val maxNitroSeconds: Float = vehicleType.baseNitroCapacity * nitroMultiplier
    var currentNitro: Float = maxNitroSeconds
    var isNitroActive: Boolean = false

    var magnetTimer: Float = 0f
    val isMagnetActive: Boolean get() = magnetTimer > 0f

    val hud = HudTelemetry()
    var isGameOver = false
    var isVictory = false
    var gameOverReason: String? = null

    var airTime: Float = 0f
    var rotationAccumulator: Float = 0f
    private var lastAngle: Float = 0f
    var totalStuntsCount: Int = 0

    val gravity: Float = world.gravity

    init {
        reset(level)
    }

    fun reset(newLevel: GeneratedLevel) {
        x = 5f
        y = newLevel.getSurfaceHeight(x) - 1.5f
        vx = 0f
        vy = 0f
        angleRad = 0f
        angularVelocity = 0f
        currentFuel = maxFuelSeconds
        currentNitro = maxNitroSeconds
        isNitroActive = false
        isGameOver = false
        isVictory = false
        gameOverReason = null
        airTime = 0f
        rotationAccumulator = 0f
        lastAngle = 0f
        totalStuntsCount = 0
        magnetTimer = 0f

        updateWheelPositions()
        rearWheel.vx = 0f
        rearWheel.vy = 0f
        frontWheel.vx = 0f
        frontWheel.vy = 0f

        hud.distanceMeters = 0f
        hud.trackLengthMeters = newLevel.finishLineX
        hud.coinsCollected = 0
        hud.diamondsCollected = 0
        hud.starsEarned = 0
        hud.currentStunt = null
        hud.comboMultiplier = 1
    }

    private fun updateWheelPositions() {
        val halfBase = vehicleType.wheelbase * 0.5f
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        val rOffsetX = -halfBase * cosA
        val rOffsetY = -halfBase * sinA
        rearWheel.x = x + rOffsetX
        rearWheel.y = y + rOffsetY + (vehicleType.chassisHeight * 0.3f)

        val fOffsetX = halfBase * cosA
        val fOffsetY = halfBase * sinA
        frontWheel.x = x + fOffsetX
        frontWheel.y = y + fOffsetY + (vehicleType.chassisHeight * 0.3f)
    }

    fun update(dt: Float, gasInput: Float, brakeInput: Float, nitroRequested: Boolean) {
        if (isGameOver || isVictory) return

        hud.timeElapsedSeconds += dt

        if (magnetTimer > 0f) {
            magnetTimer -= dt
        }

        // Fuel Consumption
        val engineLoad = if (gasInput > 0.05f) 1.0f else 0.2f
        currentFuel -= dt * engineLoad
        if (currentFuel <= 0f) {
            currentFuel = 0f
            if (abs(vx) < 0.3f && rearWheel.isGrounded) {
                triggerGameOver("Out of Fuel!")
                return
            }
        }
        hud.fuelRatio = (currentFuel / maxFuelSeconds).coerceIn(0f, 1f)
        hud.isLowFuel = hud.fuelRatio < 0.20f

        // Nitro Logic
        if (nitroRequested && currentNitro > 0f) {
            isNitroActive = true
            currentNitro -= dt * 1.8f
            if (currentNitro <= 0f) {
                currentNitro = 0f
                isNitroActive = false
            }
        } else {
            isNitroActive = false
        }
        hud.nitroRatio = (currentNitro / maxNitroSeconds).coerceIn(0f, 1f)
        hud.isNitroActive = isNitroActive

        // Dynamic Obstacle Physics Update
        updateObstacles(dt)

        val isAnyWheelGrounded = rearWheel.isGrounded || frontWheel.isGrounded

        var effectiveGas = if (currentFuel > 0f) gasInput else 0f
        if (isNitroActive) {
            effectiveGas = 1.7f
            soundEngine.playNitro()
            particleSystem.spawnNitroFlames(x, y, angleRad)
            particleSystem.triggerShake(3f)
        }

        val driveTorque = effectiveGas * vehicleType.baseEnginePower * engineMultiplier * (if (isNitroActive) 1.9f else 1.0f)
        val brakeTorque = brakeInput * 130f

        // Step Front & Rear Wheel Physics with ground friction modifiers
        stepWheelPhysics(rearWheel, dt, driveTorque, brakeTorque, isDriveWheel = true)
        stepWheelPhysics(frontWheel, dt, if (vehicleType.isHover) driveTorque else 0f, brakeTorque, isDriveWheel = false)

        // Mid-air pitch and acrobatic stunt rotation
        if (!isAnyWheelGrounded) {
            airTime += dt
            val pitchTorque = (gasInput - brakeInput) * 8.2f
            angularVelocity += pitchTorque * dt

            // Track 360 flips
            val dAngle = angleRad - lastAngle
            rotationAccumulator += dAngle
            if (abs(rotationAccumulator) >= (2.0 * Math.PI)) {
                val isFront = rotationAccumulator > 0
                val title = if (isFront) "FRONT FLIP!" else "BACK FLIP!"
                val pts = 350 * hud.comboMultiplier
                hud.comboMultiplier = (hud.comboMultiplier + 1).coerceAtMost(5)
                hud.currentStunt = StuntPopup(title, pts)
                hud.coinsCollected += pts
                totalStuntsCount++
                soundEngine.playStuntFanfare()
                particleSystem.triggerBulletTime(0.35f, 0.45f) // Bullet-time slow mo!
                particleSystem.spawnFloatingPopup(title, x, y - 2f, NeonYellow, scale = 1.4f)
                rotationAccumulator = 0f
            }
        } else {
            if (airTime > 1.1f) {
                val pts = (airTime * 160).toInt() * hud.comboMultiplier
                hud.currentStunt = StuntPopup("BIG AIR!", pts)
                hud.coinsCollected += pts
                soundEngine.playLanding()
                totalStuntsCount++
                particleSystem.spawnFloatingPopup("BIG AIR +$pts", x, y - 2f, NeonCyan)
            }
            airTime = 0f
            rotationAccumulator = 0f
        }
        lastAngle = angleRad

        // Chassis Gravity & Inertia
        vy += gravity * dt
        x += vx * dt
        y += vy * dt

        // Angular movement & damping
        angleRad += angularVelocity * dt
        angularVelocity *= (1f - (0.75f * dt))
        angularVelocity = angularVelocity.coerceIn(-12f, 12f)

        // Sync Wheels with Chassis
        updateWheelPositions()

        // Chassis collisions, obstacles, puzzles, and collectibles
        checkChassisCollision()
        checkObstacleInteractions()
        checkPuzzlesAndCollectibles(dt)

        // Finish Line Check
        if (x >= level.finishLineX && !isVictory) {
            isVictory = true
            soundEngine.playVictory()
            particleSystem.spawnFloatingPopup("STAGE CLEAR!", x, y - 3f, NeonGreen, scale = 1.6f)
        }

        // Telemetry calculation
        hud.distanceMeters = maxOf(hud.distanceMeters, x)
        hud.speedKmh = (vx * 3.6f).coerceAtLeast(0f)
        val maxKmh = vehicleType.baseMaxSpeed * 3.6f * (if (isNitroActive) 1.4f else 1.0f)
        val speedRatio = (hud.speedKmh / maxKmh).coerceIn(0f, 1f)
        hud.rpm = 1000f + (speedRatio * 7500f) + (if (effectiveGas > 0.1f) 1200f else 0f)

        // Update multi-gear engine audio
        soundEngine.updateEngineAudio(hud.speedKmh, effectiveGas)
    }

    private fun updateObstacles(dt: Float) {
        // Moving Platforms
        for (plat in level.movingPlatforms) {
            plat.update(dt)
        }

        // Laser Grids
        for (laser in level.laserGrids) {
            laser.timer += dt
        }

        // Pendulum Blades
        for (pend in level.pendulumBlades) {
            pend.timer += dt
        }

        // Rolling Boulders
        for (boulder in level.rollingBoulders) {
            boulder.vy += gravity * dt
            boulder.x += boulder.vx * dt
            boulder.y += boulder.vy * dt
            val groundY = level.getSurfaceHeight(boulder.x)
            if (boulder.y + boulder.radius >= groundY) {
                boulder.y = groundY - boulder.radius
                boulder.vy = 0f
                boulder.rotationAngle += boulder.vx * dt * 2f
            }
        }

        // Collapsing Bridges
        for (cb in level.collapsingBridges) {
            if (cb.isSteppedOn && !cb.isFallen) {
                cb.collapseTimer += dt
                if (cb.collapseTimer >= 0.7f) {
                    cb.isFallen = true
                    particleSystem.spawnDebris((cb.startX + cb.endX) * 0.5f, cb.y)
                }
            }
            if (cb.isFallen) {
                cb.fallVy += gravity * dt
                cb.fallOffsetY += cb.fallVy * dt
            }
        }

        // Falling Stalactites
        for (st in level.fallingStalactites) {
            if (!st.isTriggered && abs(x - st.x) < st.triggerDistance) {
                st.isTriggered = true
            }
            if (st.isTriggered && st.currentY < st.groundY) {
                st.vy += gravity * 1.5f * dt
                st.currentY += st.vy * dt
                if (st.currentY >= st.groundY) {
                    st.currentY = st.groundY
                    particleSystem.spawnDebris(st.x, st.groundY)
                }
            }
        }

        // Pushable Crates Physics
        for (crate in level.pushableCrates) {
            crate.vy += gravity * dt
            crate.x += crate.vx * dt
            crate.y += crate.vy * dt
            val groundY = level.getSurfaceHeight(crate.x)
            if (crate.y + crate.size * 0.5f >= groundY) {
                crate.y = groundY - crate.size * 0.5f
                crate.vy = 0f
                crate.vx *= (1f - (3f * dt))
                crate.isGrounded = true
            } else {
                crate.isGrounded = false
            }

            // Check if crate triggers Pressure Switch
            for (sw in level.switches) {
                if (!sw.isPressed && abs(crate.x - sw.x) < 1.4f && abs(crate.y - sw.y) < 1.4f) {
                    sw.isPressed = true
                    soundEngine.playStuntFanfare()
                    level.gates.find { it.switchId == sw.id }?.isOpen = true
                    particleSystem.spawnFloatingPopup("GATE UNLOCKED!", crate.x, crate.y - 2f, NeonGreen)
                }
            }
        }
    }

    private fun stepWheelPhysics(
        wheel: Wheel,
        dt: Float,
        driveTorque: Float,
        brakeTorque: Float,
        isDriveWheel: Boolean
    ) {
        var groundY = level.getSurfaceHeight(wheel.x)

        // Check Moving Platforms support
        for (plat in level.movingPlatforms) {
            if (wheel.x >= plat.currentX - plat.width * 0.5f && wheel.x <= plat.currentX + plat.width * 0.5f) {
                if (wheel.y + vehicleType.wheelRadius >= plat.currentY - 0.6f && wheel.y <= plat.currentY + 1.2f) {
                    groundY = minOf(groundY, plat.currentY)
                }
            }
        }

        // Check Seesaw Bridges support
        for (see in level.seesawBridges) {
            val halfLen = see.length * 0.5f
            if (wheel.x >= see.pivotX - halfLen && wheel.x <= see.pivotX + halfLen) {
                val dx = wheel.x - see.pivotX
                val plankY = see.pivotY + (dx * tan(see.angleRad))
                if (wheel.y + vehicleType.wheelRadius >= plankY - 0.6f && wheel.y <= plankY + 1.2f) {
                    groundY = minOf(groundY, plankY)
                    // Vehicle weight tilts the seesaw!
                    val torqueDir = if (dx > 0) 1f else -1f
                    see.angularVelocity += torqueDir * abs(dx) * 0.6f * dt
                    see.angleRad = (see.angleRad + see.angularVelocity * dt).coerceIn(-0.45f, 0.45f)
                }
            }
        }

        // Check Collapsing Bridge support
        for (cb in level.collapsingBridges) {
            if (wheel.x >= cb.startX && wheel.x <= cb.endX) {
                cb.isSteppedOn = true
                if (!cb.isFallen) {
                    groundY = minOf(groundY, cb.y)
                } else if (cb.fallOffsetY < 15f) {
                    groundY = minOf(groundY, cb.y + cb.fallOffsetY)
                }
            }
        }

        // Check Secret Elevated Path support
        for (sp in level.secretPaths) {
            if (wheel.x >= sp.startX && wheel.x <= sp.endX && wheel.y < groundY - 3f) {
                // Check height along secret path points
                val idx = ((wheel.x - sp.startX) / (sp.endX - sp.startX) * (sp.points.size - 1)).toInt().coerceIn(0, sp.points.size - 1)
                val secretY = sp.points[idx].second
                if (wheel.y + vehicleType.wheelRadius >= secretY - 0.6f && wheel.y <= secretY + 1.2f) {
                    groundY = secretY
                    if (!sp.isDiscovered) {
                        sp.isDiscovered = true
                        particleSystem.spawnFloatingPopup("SECRET PATH!", wheel.x, secretY - 2f, NeonYellow)
                    }
                }
            }
        }

        val restLength = vehicleType.chassisHeight * 0.72f
        val currentDist = (groundY - wheel.y).coerceAtLeast(0f)
        val compression = (restLength - currentDist + vehicleType.wheelRadius).coerceAtLeast(0f)
        wheel.suspensionCompression = compression

        if (compression > 0f) {
            wheel.isGrounded = true

            val stiffness = vehicleType.baseSuspensionStiffness * suspensionMultiplier
            val damping = vehicleType.baseSuspensionDamping * suspensionMultiplier
            val springForce = (compression * stiffness) - (vy * damping)

            vy -= (springForce / vehicleType.baseMass) * dt
            y = minOf(y, groundY - vehicleType.wheelRadius - 0.2f)

            // Dynamic Surface Grip Modifiers
            var surfaceFriction = vehicleType.baseTireFriction * tiresMultiplier * world.frictionMultiplier

            // Check Mud Pits (Drags speed)
            for (mud in level.mudPits) {
                if (wheel.x >= mud.x - mud.width * 0.5f && wheel.x <= mud.x + mud.width * 0.5f) {
                    surfaceFriction *= mud.dragFactor
                    vx *= (1f - (0.45f * dt))
                    particleSystem.spawnWheelDust(wheel.x, groundY, vx, Color(0xFF5D4037))
                }
            }

            // Check Ice Patches (Slippery zero-friction)
            for (ice in level.icePatches) {
                if (wheel.x >= ice.x - ice.width * 0.5f && wheel.x <= ice.x + ice.width * 0.5f) {
                    surfaceFriction *= 0.25f
                }
            }

            // Check Conveyor Belts
            for (conv in level.conveyorBelts) {
                if (wheel.x >= conv.x - conv.width * 0.5f && wheel.x <= conv.x + conv.width * 0.5f) {
                    vx += conv.beltSpeed * dt * 3f
                }
            }

            val groundSlope = level.getSurfaceSlope(wheel.x)
            val slopeAngle = atan(groundSlope)

            if (isDriveWheel && driveTorque > 0f) {
                val thrust = driveTorque * cos(slopeAngle)
                vx += (thrust / vehicleType.baseMass) * dt
                particleSystem.spawnWheelDust(wheel.x, groundY, vx, world.groundSurfaceColor)
            }

            if (brakeTorque > 0f) {
                val brakeDecel = brakeTorque * surfaceFriction / vehicleType.baseMass
                if (vx > 0f) {
                    vx = (vx - brakeDecel * dt).coerceAtLeast(0f)
                } else {
                    vx = (vx + brakeDecel * dt).coerceAtMost(0f)
                }
                if (abs(vx) > 8f) {
                    soundEngine.playTireScreech()
                }
            }

            // Slope pull
            val slopePull = sin(slopeAngle) * gravity * 0.42f
            vx += slopePull * dt

            // Ground friction damping
            vx *= (1f - (0.07f * dt * surfaceFriction))

            // Realistic wheel rotation matching ground speed
            wheel.angularVelocity = vx / vehicleType.wheelRadius
        } else {
            wheel.isGrounded = false
            if (driveTorque > 0f) {
                wheel.angularVelocity += 16f * dt
            }
            wheel.angularVelocity *= (1f - (0.4f * dt))
        }

        wheel.rotationAngle += wheel.angularVelocity * dt
    }

    private fun checkObstacleInteractions() {
        // Speed Boost Pads
        for (pad in level.speedBoostPads) {
            if (abs(x - pad.x) < pad.width * 0.5f && abs(y - pad.y) < 2.0f) {
                vx = maxOf(vx, pad.boostSpeed)
                soundEngine.playBoostPad()
                particleSystem.spawnNitroFlames(x, y, angleRad)
                particleSystem.spawnFloatingPopup("TURBO BOOST!", x, y - 2f, NeonCyan)
            }
        }

        // Launch Springs
        for (spring in level.launchSprings) {
            if (abs(x - spring.x) < spring.width * 0.5f && abs(y - spring.y) < 1.8f) {
                vy = spring.impulseVy
                vx = maxOf(vx, 16f)
                spring.isTriggered = true
                soundEngine.playSpringBounce()
                particleSystem.spawnLandingImpact(x, y, NeonYellow)
                particleSystem.spawnFloatingPopup("LAUNCH!", x, y - 2f, NeonYellow)
            }
        }

        // Bouncy Mushrooms
        for (mush in level.bouncyMushrooms) {
            if (abs(x - mush.x) < mush.width * 0.5f && abs(y - mush.y) < 1.8f) {
                vy = mush.bounceImpulse
                soundEngine.playSpringBounce()
                particleSystem.spawnLandingImpact(x, y, NeonPink)
                particleSystem.spawnFloatingPopup("BOING!", x, y - 2f, NeonPink)
            }
        }

        // Wind Tunnels
        for (wind in level.windTunnels) {
            if (x >= wind.startX && x <= wind.endX && y >= wind.startY && y <= wind.endY) {
                vx += wind.pushVx * 0.05f
                vy += wind.pushVy * 0.05f
            }
        }

        // Explosive Barrels
        for (bar in level.explosiveBarrels) {
            if (!bar.isExploded && hypot(x - bar.x, y - bar.y) < 2.2f) {
                bar.isExploded = true
                vy = -18f
                vx += 10f
                soundEngine.playCrash()
                particleSystem.spawnExplosion(bar.x, bar.y)
                particleSystem.spawnFloatingPopup("BOOM!", bar.x, bar.y - 2f, DangerRed)
            }
        }

        // Pushable Crates pushing
        for (crate in level.pushableCrates) {
            if (hypot(x - crate.x, y - crate.y) < 2.6f) {
                val pushDir = if (x < crate.x) 1f else -1f
                crate.vx = pushDir * (abs(vx) + 4f)
                crate.vy = -3f
                vx *= 0.75f
                soundEngine.playLanding()
            }
        }

        // Mystery Chests
        for (chest in level.mysteryChests) {
            if (!chest.isOpened && hypot(x - chest.x, y - chest.y) < 2.6f) {
                chest.isOpened = true
                hud.coinsCollected += chest.coinsReward
                hud.diamondsCollected += chest.diamondsReward
                soundEngine.playChestUnlock()
                particleSystem.spawnCollectSparkles(chest.x, chest.y, isDiamond = true)
                particleSystem.spawnFloatingPopup("SECRET CHEST +${chest.coinsReward}!", chest.x, chest.y - 2f, NeonYellow, scale = 1.4f)
            }
        }

        // Rolling Boulder Collision (Heavy Crash if head on)
        for (boulder in level.rollingBoulders) {
            if (hypot(x - boulder.x, y - boulder.y) < (boulder.radius + 1.2f)) {
                triggerGameOver("Crushed by Rolling Boulder!")
                particleSystem.spawnExplosion(x, y)
                soundEngine.playCrash()
                return
            }
        }

        // Laser Grid Danger
        for (laser in level.laserGrids) {
            if (laser.isDangerous && abs(x - laser.x) < 1.0f && y > laser.y - laser.height) {
                triggerGameOver("Vaporized by Laser Grid!")
                particleSystem.spawnExplosion(x, y)
                soundEngine.playCrash()
                return
            }
        }

        // Pendulum Blade Danger
        for (pend in level.pendulumBlades) {
            if (hypot(x - pend.bladeX, y - pend.bladeY) < (pend.bladeRadius + 1.0f)) {
                triggerGameOver("Sliced by Pendulum Blade!")
                particleSystem.spawnExplosion(x, y)
                soundEngine.playCrash()
                return
            }
        }

        // Closed Gate collision
        for (gate in level.gates) {
            if (!gate.isOpen && abs(x - gate.x) < 1.0f && y > gate.y - gate.height) {
                vx = -abs(vx) * 0.35f
                x = gate.x - 1.2f
                particleSystem.spawnSparks(x, y)
                soundEngine.playCrash()
            }
        }
    }

    private fun checkPuzzlesAndCollectibles(dt: Float) {
        val collectRadius = 2.4f
        val magnetRadius = if (isMagnetActive) 16f else 0f

        for (item in level.collectibles) {
            if (item.isCollected) continue

            val dist = hypot(x - item.x, y - item.y)

            // Magnet Attraction: pull coins toward vehicle
            if (isMagnetActive && (item.type == CollectibleType.COIN || item.type == CollectibleType.DIAMOND) && dist < magnetRadius) {
                item.isAttracted = true
                val dirX = (x - item.x) / dist
                val dirY = (y - item.y) / dist
                item.vx += dirX * 28f * dt
                item.vy += dirY * 28f * dt
                item.x += item.vx * dt
                item.y += item.vy * dt
            }

            if (dist < collectRadius) {
                item.isCollected = true
                when (item.type) {
                    CollectibleType.COIN -> {
                        hud.coinsCollected += item.value
                        soundEngine.playCoin()
                        particleSystem.spawnCollectSparkles(item.x, item.y, isDiamond = false)
                        particleSystem.spawnFloatingPopup("+${item.value}", item.x, item.y - 1f, GoldCoin)
                    }
                    CollectibleType.DIAMOND -> {
                        hud.diamondsCollected += item.value
                        soundEngine.playDiamond()
                        particleSystem.spawnCollectSparkles(item.x, item.y, isDiamond = true)
                        particleSystem.spawnFloatingPopup("+${item.value} 💎", item.x, item.y - 1.2f, DiamondBlue, scale = 1.3f)
                    }
                    CollectibleType.FUEL -> {
                        currentFuel = maxFuelSeconds
                        soundEngine.playFuel()
                        particleSystem.spawnCollectSparkles(item.x, item.y, isDiamond = false)
                        particleSystem.spawnFloatingPopup("FUEL 100%!", item.x, item.y - 1.2f, NeonGreen)
                    }
                    CollectibleType.NITRO -> {
                        currentNitro = maxNitroSeconds
                        soundEngine.playFuel()
                        particleSystem.spawnCollectSparkles(item.x, item.y, isDiamond = true)
                        particleSystem.spawnFloatingPopup("NITRO FULL!", item.x, item.y - 1.2f, NeonCyan)
                    }
                    CollectibleType.MAGNET -> {
                        magnetTimer = 12f // 12 seconds magnet power!
                        soundEngine.playChestUnlock()
                        particleSystem.spawnCollectSparkles(item.x, item.y, isDiamond = true)
                        particleSystem.spawnFloatingPopup("COIN MAGNET 🧲", item.x, item.y - 1.5f, NeonPink, scale = 1.4f)
                    }
                    CollectibleType.MYSTERY_CHEST -> {
                        hud.coinsCollected += 300
                        soundEngine.playChestUnlock()
                        particleSystem.spawnCollectSparkles(item.x, item.y, isDiamond = true)
                    }
                }
            }
        }

        // Stars Check
        for (star in level.stars) {
            if (star.isCollected) continue
            val dist = hypot(x - star.x, y - star.y)
            if (dist < 2.6f) {
                star.isCollected = true
                hud.starsEarned++
                soundEngine.playDiamond()
                particleSystem.spawnCollectSparkles(star.x, star.y, isDiamond = true)
                particleSystem.spawnFloatingPopup("STAR #${star.index} ⭐", star.x, star.y - 2f, NeonYellow, scale = 1.5f)
            }
        }

        // Pressure Switches Check
        for (sw in level.switches) {
            if (!sw.isPressed && abs(x - sw.x) < 1.6f && y >= sw.y - 1.2f) {
                sw.isPressed = true
                soundEngine.playStuntFanfare()
                level.gates.find { it.switchId == sw.id }?.isOpen = true
                particleSystem.spawnFloatingPopup("SWITCH PRESSED!", sw.x, sw.y - 1.5f, NeonGreen)
            }
        }
    }

    private fun checkChassisCollision() {
        val groundY = level.getSurfaceHeight(x)
        val chassisTopY = y - (vehicleType.chassisHeight * 0.6f)
        val chassisBottomY = y + (vehicleType.chassisHeight * 0.6f)

        val isUpsideDown = cos(angleRad) < -0.3f
        if (isUpsideDown && (chassisTopY >= groundY - 0.3f || chassisBottomY >= groundY)) {
            triggerGameOver("Driver Impact Crash!")
            particleSystem.spawnExplosion(x, y)
            soundEngine.playCrash()
            return
        }

        if (y > groundY + 12f) {
            triggerGameOver("Fell into the Abyss!")
            soundEngine.playCrash()
            return
        }

        // Spikes collision
        for (spike in level.spikes) {
            if (x >= spike.x - spike.width * 0.5f && x <= spike.x + spike.width * 0.5f) {
                if (y >= spike.y - spike.height) {
                    triggerGameOver("Impaled on Spike Trap!")
                    particleSystem.spawnExplosion(x, y)
                    soundEngine.playCrash()
                    return
                }
            }
        }
    }

    fun triggerGameOver(reason: String) {
        if (isGameOver || isVictory) return
        isGameOver = true
        gameOverReason = reason
        particleSystem.triggerShake(18f)
        prefs.recordGameStats(x, totalStuntsCount, hud.speedKmh)
    }
}
