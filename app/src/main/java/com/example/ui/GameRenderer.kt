package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.*
import com.example.physics.GeneratedLevel
import com.example.physics.PhysicsEngine
import com.example.ui.theme.*
import com.example.vfx.ParticleSystem
import kotlin.math.*

object GameRenderer {

    fun drawWorld(
        drawScope: DrawScope,
        physics: PhysicsEngine,
        level: GeneratedLevel,
        world: WorldInfo,
        particles: ParticleSystem,
        cameraX: Float,
        cameraY: Float,
        zoom: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val centerX = viewWidth * 0.5f
        val centerY = viewHeight * 0.5f

        fun worldToScreen(wx: Float, wy: Float): Offset {
            val sx = centerX + (wx - cameraX) * zoom
            val sy = centerY + (wy - cameraY) * zoom
            return Offset(sx, sy)
        }

        // Visible x bounds for culling optimization
        val minVisibleX = cameraX - (viewWidth / zoom) * 0.65f
        val maxVisibleX = cameraX + (viewWidth / zoom) * 0.65f

        // 1. Draw Sky Gradient
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(world.skyGradientTop, world.skyGradientBottom)
            ),
            topLeft = Offset.Zero,
            size = Size(viewWidth, viewHeight)
        )

        // 2. Parallax Backdrop
        drawParallaxBackdrop(drawScope, world, cameraX, viewWidth, viewHeight)

        // 3. Draw Terrain Geometry
        drawTerrain(drawScope, level, world, ::worldToScreen, viewWidth, viewHeight, minVisibleX, maxVisibleX)

        // 4. Draw Secret Paths
        drawSecretPaths(drawScope, level, ::worldToScreen, minVisibleX, maxVisibleX)

        // 5. Draw Interactive Obstacles & Puzzles (20 Obstacles!)
        drawAdvancedObstacles(drawScope, level, ::worldToScreen, minVisibleX, maxVisibleX)

        // 6. Draw Collectibles & Stars
        drawCollectibles(drawScope, level, ::worldToScreen, minVisibleX, maxVisibleX)

        // 7. Draw Vehicle & Wheels
        drawVehicle(drawScope, physics, ::worldToScreen)

        // 8. Draw Finish Line Banner
        drawFinishLine(drawScope, level.finishLineX, level.getSurfaceHeight(level.finishLineX), ::worldToScreen)

        // 9. Draw Particles & Shockwaves
        particles.draw(drawScope, ::worldToScreen)

        // 10. Draw Cinematic Speed Lines & Vignette
        drawCinematicEffects(drawScope, physics, viewWidth, viewHeight)

        // 11. Draw Floating Popups
        drawFloatingPopups(drawScope, particles, ::worldToScreen)
    }

    private fun drawParallaxBackdrop(
        drawScope: DrawScope,
        world: WorldInfo,
        cameraX: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val path = Path()
        val baseH = viewHeight * 0.65f
        val layer1Offset = cameraX * 8f % viewWidth

        path.moveTo(0f, viewHeight)
        path.lineTo(0f, baseH)
        var x = 0f
        while (x <= viewWidth + 40f) {
            val h = sin((x + layer1Offset) * 0.005f) * 60f + cos((x + layer1Offset) * 0.01f) * 40f
            path.lineTo(x, baseH + h)
            x += 40f
        }
        path.lineTo(viewWidth, viewHeight)
        path.close()

        drawScope.drawPath(
            path = path,
            color = world.groundFillColor.copy(alpha = 0.35f)
        )
    }

    private fun drawTerrain(
        drawScope: DrawScope,
        level: GeneratedLevel,
        world: WorldInfo,
        w2s: (Float, Float) -> Offset,
        viewWidth: Float,
        viewHeight: Float,
        minX: Float,
        maxX: Float
    ) {
        if (level.points.isEmpty()) return

        val terrainPath = Path()
        val borderPath = Path()

        // Filter points within view +- margin for optimized rendering
        val visiblePoints = level.points.filter { it.first >= minX - 10f && it.first <= maxX + 10f }
        if (visiblePoints.isEmpty()) return

        var first = true
        for (pt in visiblePoints) {
            val sc = w2s(pt.first, pt.second)
            if (first) {
                terrainPath.moveTo(sc.x, sc.y)
                borderPath.moveTo(sc.x, sc.y)
                first = false
            } else {
                terrainPath.lineTo(sc.x, sc.y)
                borderPath.lineTo(sc.x, sc.y)
            }
        }

        val lastSc = w2s(visiblePoints.last().first, visiblePoints.last().second)
        val firstSc = w2s(visiblePoints.first().first, visiblePoints.first().second)
        terrainPath.lineTo(lastSc.x, viewHeight + 600f)
        terrainPath.lineTo(firstSc.x, viewHeight + 600f)
        terrainPath.close()

        drawScope.drawPath(
            path = terrainPath,
            brush = Brush.verticalGradient(
                colors = listOf(world.groundSurfaceColor.copy(alpha = 0.95f), world.groundFillColor, world.rockDetailColor)
            )
        )

        drawScope.drawPath(
            path = borderPath,
            color = world.groundSurfaceColor,
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    private fun drawSecretPaths(
        drawScope: DrawScope,
        level: GeneratedLevel,
        w2s: (Float, Float) -> Offset,
        minX: Float,
        maxX: Float
    ) {
        for (sp in level.secretPaths) {
            if (sp.endX < minX || sp.startX > maxX) continue
            val path = Path()
            var first = true
            for (pt in sp.points) {
                val sc = w2s(pt.first, pt.second)
                if (first) {
                    path.moveTo(sc.x, sc.y)
                    first = false
                } else {
                    path.lineTo(sc.x, sc.y)
                }
            }
            drawScope.drawPath(
                path = path,
                color = if (sp.isDiscovered) NeonCyan else NeonYellow.copy(alpha = 0.6f),
                style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f)))
            )
        }
    }

    private fun drawAdvancedObstacles(
        drawScope: DrawScope,
        level: GeneratedLevel,
        w2s: (Float, Float) -> Offset,
        minX: Float,
        maxX: Float
    ) {
        val now = System.currentTimeMillis()

        // 1. Speed Boost Pads
        for (pad in level.speedBoostPads) {
            if (pad.x < minX || pad.x > maxX) continue
            val sc = w2s(pad.x, pad.y)
            drawScope.drawRoundRect(
                color = NeonYellow,
                topLeft = Offset(sc.x - 30f, sc.y - 6f),
                size = Size(60f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            // Chevrons
            for (c in -1..1) {
                drawScope.drawLine(
                    color = Color.Black,
                    start = Offset(sc.x + (c * 18f) - 6f, sc.y - 4f),
                    end = Offset(sc.x + (c * 18f) + 6f, sc.y),
                    strokeWidth = 3f
                )
                drawScope.drawLine(
                    color = Color.Black,
                    start = Offset(sc.x + (c * 18f) + 6f, sc.y),
                    end = Offset(sc.x + (c * 18f) - 6f, sc.y + 4f),
                    strokeWidth = 3f
                )
            }
        }

        // 2. Launch Springs
        for (spring in level.launchSprings) {
            if (spring.x < minX || spring.x > maxX) continue
            val sc = w2s(spring.x, spring.y)
            val h = if (spring.isTriggered) 8f else 18f
            // Coil spring
            drawScope.drawRoundRect(
                color = DangerRed,
                topLeft = Offset(sc.x - 22f, sc.y - h),
                size = Size(44f, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawScope.drawRect(
                color = NeonYellow,
                topLeft = Offset(sc.x - 24f, sc.y - h - 4f),
                size = Size(48f, 6f)
            )
        }

        // 3. Seesaw Bridges
        for (see in level.seesawBridges) {
            if (see.pivotX < minX - 10f || see.pivotX > maxX + 10f) continue
            val pSc = w2s(see.pivotX, see.pivotY)
            // Draw pivot stand
            val tri = Path().apply {
                moveTo(pSc.x, pSc.y)
                lineTo(pSc.x - 14f, pSc.y + 20f)
                lineTo(pSc.x + 14f, pSc.y + 20f)
                close()
            }
            drawScope.drawPath(tri, color = Color.DarkGray)

            // Draw rotating plank
            val deg = Math.toDegrees(see.angleRad.toDouble()).toFloat()
            drawScope.rotate(degrees = deg, pivot = pSc) {
                drawScope.drawRoundRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(pSc.x - 110f, pSc.y - 6f),
                    size = Size(220f, 12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                drawScope.drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(pSc.x - 110f, pSc.y - 6f),
                    size = Size(220f, 12f),
                    style = Stroke(width = 2f)
                )
            }
        }

        // 4. Collapsing Bridges
        for (cb in level.collapsingBridges) {
            if (cb.endX < minX || cb.startX > maxX) continue
            val lSc = w2s(cb.startX, cb.y + cb.fallOffsetY)
            val rSc = w2s(cb.endX, cb.y + cb.fallOffsetY)
            val shake = if (cb.isSteppedOn && !cb.isFallen) (sin(now * 0.05) * 3f).toFloat() else 0f

            drawScope.drawRoundRect(
                color = if (cb.isSteppedOn) DangerRed.copy(alpha = 0.85f) else Color(0xFF78716C),
                topLeft = Offset(lSc.x, lSc.y - 6f + shake),
                size = Size(rSc.x - lSc.x, 14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }

        // 5. Rolling Boulders
        for (boulder in level.rollingBoulders) {
            if (boulder.x < minX || boulder.x > maxX) continue
            val sc = w2s(boulder.x, boulder.y)
            val r = boulder.radius * 16f
            val deg = Math.toDegrees(boulder.rotationAngle.toDouble()).toFloat()

            drawScope.drawCircle(color = Color(0xFF475569), radius = r, center = sc)
            drawScope.drawCircle(color = Color(0xFF1E293B), radius = r, center = sc, style = Stroke(width = 3f))

            drawScope.rotate(degrees = deg, pivot = sc) {
                drawScope.drawLine(color = Color.Black, start = Offset(sc.x - r * 0.7f, sc.y), end = Offset(sc.x + r * 0.7f, sc.y), strokeWidth = 3f)
                drawScope.drawLine(color = Color.Black, start = Offset(sc.x, sc.y - r * 0.7f), end = Offset(sc.x, sc.y + r * 0.7f), strokeWidth = 3f)
            }
        }

        // 6. Laser Grids
        for (laser in level.laserGrids) {
            if (laser.x < minX || laser.x > maxX) continue
            val baseSc = w2s(laser.x, laser.y)
            val topSc = w2s(laser.x, laser.y - laser.height)
            val isDeadly = laser.isDangerous

            drawScope.drawLine(
                color = if (isDeadly) DangerRed else NeonGreen.copy(alpha = 0.3f),
                start = baseSc,
                end = topSc,
                strokeWidth = if (isDeadly) 6f else 2f
            )
            // Emitter bulb
            drawScope.drawCircle(color = if (isDeadly) DangerRed else NeonGreen, radius = 7f, center = topSc)
        }

        // 7. Pendulum Blades
        for (pend in level.pendulumBlades) {
            if (pend.pivotX < minX - 10f || pend.pivotX > maxX + 10f) continue
            val pivSc = w2s(pend.pivotX, pend.pivotY)
            val bladeSc = w2s(pend.bladeX, pend.bladeY)

            drawScope.drawLine(color = Color.Gray, start = pivSc, end = bladeSc, strokeWidth = 4f)
            drawScope.drawCircle(color = DangerRed, radius = pend.bladeRadius * 14f, center = bladeSc)
            drawScope.drawCircle(color = Color.White, radius = 5f, center = bladeSc)
        }

        // 8. Wind Tunnels
        for (wind in level.windTunnels) {
            if (wind.endX < minX || wind.startX > maxX) continue
            val lSc = w2s(wind.startX, wind.endY)
            val rSc = w2s(wind.endX, wind.startY)
            drawScope.drawRect(
                color = NeonCyan.copy(alpha = 0.12f),
                topLeft = Offset(lSc.x, rSc.y),
                size = Size(rSc.x - lSc.x, lSc.y - rSc.y)
            )
        }

        // 9. Explosive Barrels
        for (bar in level.explosiveBarrels) {
            if (bar.isExploded || bar.x < minX || bar.x > maxX) continue
            val sc = w2s(bar.x, bar.y)
            drawScope.drawRoundRect(
                color = DangerRed,
                topLeft = Offset(sc.x - 12f, sc.y - 18f),
                size = Size(24f, 36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawScope.drawCircle(color = NeonYellow, radius = 4f, center = sc)
        }

        // 10. Pushable Crates
        for (crate in level.pushableCrates) {
            if (crate.x < minX || crate.x > maxX) continue
            val sc = w2s(crate.x, crate.y)
            val s = crate.size * 14f
            drawScope.drawRoundRect(
                color = Color(0xFF854D0E),
                topLeft = Offset(sc.x - s * 0.5f, sc.y - s * 0.5f),
                size = Size(s, s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
            drawScope.drawRect(
                color = Color(0xFFA16207),
                topLeft = Offset(sc.x - s * 0.5f, sc.y - s * 0.5f),
                size = Size(s, s),
                style = Stroke(width = 2.5f)
            )
        }

        // 11. Mud Pits & Ice Patches
        for (mud in level.mudPits) {
            if (mud.x < minX || mud.x > maxX) continue
            val sc = w2s(mud.x, mud.y)
            drawScope.drawRoundRect(
                color = Color(0xFF451A03).copy(alpha = 0.85f),
                topLeft = Offset(sc.x - 45f, sc.y - 4f),
                size = Size(90f, 10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }

        for (ice in level.icePatches) {
            if (ice.x < minX || ice.x > maxX) continue
            val sc = w2s(ice.x, ice.y)
            drawScope.drawRoundRect(
                color = Color(0xFFBAE6FD).copy(alpha = 0.75f),
                topLeft = Offset(sc.x - 50f, sc.y - 5f),
                size = Size(100f, 10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }

        // 12. Conveyor Belts
        for (conv in level.conveyorBelts) {
            if (conv.x < minX || conv.x > maxX) continue
            val sc = w2s(conv.x, conv.y)
            drawScope.drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(sc.x - 45f, sc.y - 6f),
                size = Size(90f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawScope.drawRoundRect(
                color = NeonCyan,
                topLeft = Offset(sc.x - 45f, sc.y - 6f),
                size = Size(90f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                style = Stroke(width = 2f)
            )
        }

        // 13. Bouncy Mushrooms
        for (mush in level.bouncyMushrooms) {
            if (mush.x < minX || mush.x > maxX) continue
            val sc = w2s(mush.x, mush.y)
            drawScope.drawArc(
                color = NeonPink,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(sc.x - 22f, sc.y - 20f),
                size = Size(44f, 28f)
            )
            drawScope.drawCircle(color = Color.White, radius = 3.5f, center = Offset(sc.x - 8f, sc.y - 10f))
            drawScope.drawCircle(color = Color.White, radius = 3.5f, center = Offset(sc.x + 8f, sc.y - 10f))
        }

        // 14. Falling Stalactites
        for (st in level.fallingStalactites) {
            if (st.x < minX || st.x > maxX) continue
            val sc = w2s(st.x, st.currentY)
            val stPath = Path().apply {
                moveTo(sc.x - 10f, sc.y - 24f)
                lineTo(sc.x + 10f, sc.y - 24f)
                lineTo(sc.x, sc.y)
                close()
            }
            drawScope.drawPath(stPath, color = Color(0xFF64748B))
        }

        // 15. Mystery Chests
        for (chest in level.mysteryChests) {
            if (chest.isOpened || chest.x < minX || chest.x > maxX) continue
            val sc = w2s(chest.x, chest.y)
            drawScope.drawRoundRect(
                color = GoldCoin,
                topLeft = Offset(sc.x - 18f, sc.y - 16f),
                size = Size(36f, 30f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawScope.drawCircle(color = DiamondBlue, radius = 5f, center = Offset(sc.x, sc.y - 2f))
        }

        // Moving Platforms & Spikes & Gates
        for (plat in level.movingPlatforms) {
            if (plat.currentX < minX - 10f || plat.currentX > maxX + 10f) continue
            val leftSc = w2s(plat.currentX - plat.width * 0.5f, plat.currentY)
            val rightSc = w2s(plat.currentX + plat.width * 0.5f, plat.currentY)
            val pWidth = rightSc.x - leftSc.x
            drawScope.drawRoundRect(
                color = CardNavy,
                topLeft = Offset(leftSc.x, leftSc.y - 9f),
                size = Size(pWidth, 18f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawScope.drawRoundRect(
                color = NeonCyan,
                topLeft = Offset(leftSc.x, leftSc.y - 9f),
                size = Size(pWidth, 18f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                style = Stroke(width = 3f)
            )
        }

        for (spk in level.spikes) {
            if (spk.x < minX - 10f || spk.x > maxX + 10f) continue
            val leftSc = w2s(spk.x - spk.width * 0.5f, spk.y)
            val rightSc = w2s(spk.x + spk.width * 0.5f, spk.y)
            val width = rightSc.x - leftSc.x
            val teeth = 6
            val toothW = width / teeth
            for (i in 0 until teeth) {
                val tx = leftSc.x + (i * toothW)
                val spikePath = Path().apply {
                    moveTo(tx, leftSc.y)
                    lineTo(tx + toothW * 0.5f, leftSc.y - 24f)
                    lineTo(tx + toothW, leftSc.y)
                    close()
                }
                drawScope.drawPath(spikePath, color = DangerRed)
            }
        }
    }

    private fun drawCollectibles(
        drawScope: DrawScope,
        level: GeneratedLevel,
        w2s: (Float, Float) -> Offset,
        minX: Float,
        maxX: Float
    ) {
        val now = System.currentTimeMillis()
        val pulse = sin(now * 0.006f) * 3f

        for (item in level.collectibles) {
            if (item.isCollected || item.x < minX - 5f || item.x > maxX + 5f) continue
            val sc = w2s(item.x, item.y)

            when (item.type) {
                CollectibleType.COIN -> {
                    drawScope.drawCircle(color = GoldCoin, radius = 12f + pulse, center = sc)
                    drawScope.drawCircle(color = Color(0xFFFFE082), radius = 8f + pulse * 0.5f, center = sc)
                }
                CollectibleType.DIAMOND -> {
                    val dSize = 14f + pulse
                    val gemPath = Path().apply {
                        moveTo(sc.x, sc.y - dSize)
                        lineTo(sc.x + dSize, sc.y)
                        lineTo(sc.x, sc.y + dSize)
                        lineTo(sc.x - dSize, sc.y)
                        close()
                    }
                    drawScope.drawPath(gemPath, color = DiamondBlue)
                    drawScope.drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 4f, center = Offset(sc.x - 3f, sc.y - 3f))
                }
                CollectibleType.FUEL -> {
                    drawScope.drawRoundRect(
                        color = DangerRed,
                        topLeft = Offset(sc.x - 11f, sc.y - 15f),
                        size = Size(22f, 30f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    drawScope.drawRect(color = Color.White, topLeft = Offset(sc.x - 6f, sc.y - 8f), size = Size(12f, 16f))
                }
                CollectibleType.NITRO -> {
                    drawScope.drawRoundRect(
                        color = NeonCyan,
                        topLeft = Offset(sc.x - 9f, sc.y - 16f),
                        size = Size(18f, 32f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
                CollectibleType.MAGNET -> {
                    drawScope.drawArc(
                        color = NeonPink,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(sc.x - 14f, sc.y - 14f),
                        size = Size(28f, 28f),
                        style = Stroke(width = 7f)
                    )
                }
                else -> {}
            }
        }

        // Stars
        for (star in level.stars) {
            if (star.isCollected || star.x < minX - 5f || star.x > maxX + 5f) continue
            val sc = w2s(star.x, star.y)
            drawStar(drawScope, sc.x, sc.y, 18f + pulse, NeonYellow)
        }
    }

    private fun drawStar(drawScope: DrawScope, cx: Float, cy: Float, radius: Float, color: Color) {
        val path = Path()
        val innerRadius = radius * 0.45f
        var angle = -Math.PI / 2.0
        val step = Math.PI / 5.0
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) radius else innerRadius
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += step
        }
        path.close()
        drawScope.drawPath(path, color = color)
        drawScope.drawPath(path, color = Color.White, style = Stroke(width = 2.5f))
    }

    private fun drawVehicle(
        drawScope: DrawScope,
        physics: PhysicsEngine,
        w2s: (Float, Float) -> Offset
    ) {
        val carCenter = w2s(physics.x, physics.y)
        val rearWheelCenter = w2s(physics.rearWheel.x, physics.rearWheel.y)
        val frontWheelCenter = w2s(physics.frontWheel.x, physics.frontWheel.y)
        val degrees = Math.toDegrees(physics.angleRad.toDouble()).toFloat()

        // Suspension struts
        drawScope.drawLine(color = Color(0xFF64748B), start = carCenter, end = rearWheelCenter, strokeWidth = 6f, cap = StrokeCap.Round)
        drawScope.drawLine(color = Color(0xFF64748B), start = carCenter, end = frontWheelCenter, strokeWidth = 6f, cap = StrokeCap.Round)

        // Wheels
        val rRadius = physics.vehicleType.wheelRadius * 36f
        drawWheel(drawScope, rearWheelCenter, rRadius, physics.rearWheel.rotationAngle)
        drawWheel(drawScope, frontWheelCenter, rRadius, physics.frontWheel.rotationAngle)

        // Chassis with Rotation
        drawScope.rotate(degrees = degrees, pivot = carCenter) {
            val w = physics.vehicleType.chassisWidth * 26f
            val h = physics.vehicleType.chassisHeight * 24f

            // Neon Underglow
            drawScope.drawRoundRect(
                color = NeonCyan.copy(alpha = 0.55f),
                topLeft = Offset(carCenter.x - w * 0.55f, carCenter.y + h * 0.35f),
                size = Size(w * 1.1f, 10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
            )

            // Body
            val bodyPath = Path().apply {
                moveTo(carCenter.x - w * 0.5f, carCenter.y + h * 0.3f)
                lineTo(carCenter.x - w * 0.45f, carCenter.y - h * 0.2f)
                lineTo(carCenter.x - w * 0.15f, carCenter.y - h * 0.5f)
                lineTo(carCenter.x + w * 0.2f, carCenter.y - h * 0.5f)
                lineTo(carCenter.x + w * 0.48f, carCenter.y + h * 0.1f)
                lineTo(carCenter.x + w * 0.5f, carCenter.y + h * 0.3f)
                close()
            }

            drawScope.drawPath(
                path = bodyPath,
                brush = Brush.linearGradient(colors = listOf(NeonCyan, Color(0xFF0284C7)))
            )
            drawScope.drawPath(path = bodyPath, color = Color.White, style = Stroke(width = 2.5f))

            // Cockpit Window
            val windowPath = Path().apply {
                moveTo(carCenter.x - w * 0.15f, carCenter.y - h * 0.45f)
                lineTo(carCenter.x + w * 0.15f, carCenter.y - h * 0.45f)
                lineTo(carCenter.x + w * 0.3f, carCenter.y - h * 0.05f)
                lineTo(carCenter.x - w * 0.12f, carCenter.y - h * 0.05f)
                close()
            }
            drawScope.drawPath(windowPath, color = Color(0xFF0F172A).copy(alpha = 0.85f))

            // Driver Helmet
            drawScope.drawCircle(color = NeonOrange, radius = 8f, center = Offset(carCenter.x, carCenter.y - h * 0.22f))
            drawScope.drawCircle(color = NeonYellow, radius = 5f, center = Offset(carCenter.x + w * 0.47f, carCenter.y + h * 0.15f))
        }
    }

    private fun drawWheel(drawScope: DrawScope, center: Offset, radius: Float, angleRad: Float) {
        drawScope.drawCircle(color = Color(0xFF1E293B), radius = radius, center = center)
        drawScope.drawCircle(color = Color.Black, radius = radius, center = center, style = Stroke(width = radius * 0.32f))
        drawScope.drawCircle(color = NeonCyan, radius = radius * 0.65f, center = center, style = Stroke(width = 4f))

        val deg = Math.toDegrees(angleRad.toDouble()).toFloat()
        drawScope.rotate(degrees = deg, pivot = center) {
            val spokeLen = radius * 0.6f
            for (i in 0 until 4) {
                val a = (i * Math.PI / 2.0).toFloat()
                drawScope.drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x + spokeLen * cos(a), center.y + spokeLen * sin(a)),
                    strokeWidth = 3.5f
                )
            }
        }
        drawScope.drawCircle(color = NeonYellow, radius = radius * 0.22f, center = center)
    }

    private fun drawFinishLine(
        drawScope: DrawScope,
        finishX: Float,
        surfaceY: Float,
        w2s: (Float, Float) -> Offset
    ) {
        val baseSc = w2s(finishX, surfaceY)
        val archH = 140f
        drawScope.drawRect(color = Color.White, topLeft = Offset(baseSc.x - 8f, baseSc.y - archH), size = Size(16f, archH))

        val rows = 3
        val cols = 8
        val cellW = 12f
        val cellH = 12f
        val startX = baseSc.x - 48f
        val startY = baseSc.y - archH - 36f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isBlack = (r + c) % 2 == 0
                drawScope.drawRect(
                    color = if (isBlack) Color.Black else Color.White,
                    topLeft = Offset(startX + c * cellW, startY + r * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }
    }

    private fun drawCinematicEffects(
        drawScope: DrawScope,
        physics: PhysicsEngine,
        viewWidth: Float,
        viewHeight: Float
    ) {
        // Speed lines at high speed
        if (physics.hud.speedKmh > 85f || physics.isNitroActive) {
            val alpha = ((physics.hud.speedKmh - 85f) / 50f).coerceIn(0.1f, 0.45f)
            val strokeColor = (if (physics.isNitroActive) NeonCyan else Color.White).copy(alpha = alpha)

            // Radial speed streaks
            for (i in 0 until 12) {
                val angle = (i * Math.PI / 6.0).toFloat()
                val startX = (viewWidth * 0.5f) + cos(angle) * (viewWidth * 0.38f)
                val startY = (viewHeight * 0.5f) + sin(angle) * (viewHeight * 0.38f)
                val endX = (viewWidth * 0.5f) + cos(angle) * (viewWidth * 0.55f)
                val endY = (viewHeight * 0.5f) + sin(angle) * (viewHeight * 0.55f)
                drawScope.drawLine(color = strokeColor, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 3f)
            }
        }

        // Crash red vignette
        if (physics.isGameOver) {
            drawScope.drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, DangerRed.copy(alpha = 0.5f)),
                    center = Offset(viewWidth * 0.5f, viewHeight * 0.5f),
                    radius = viewWidth * 0.7f
                )
            )
        }
    }

    private fun drawFloatingPopups(
        drawScope: DrawScope,
        particles: ParticleSystem,
        w2s: (Float, Float) -> Offset
    ) {
        for (pop in particles.getActivePopups()) {
            val sc = w2s(pop.x, pop.y)
            val alpha = (pop.life / pop.maxLife).coerceIn(0f, 1f)
            // Floating badge backing
            drawScope.drawCircle(
                color = pop.color.copy(alpha = alpha * 0.9f),
                radius = 16f * pop.scale * alpha,
                center = sc
            )
        }
    }
}
