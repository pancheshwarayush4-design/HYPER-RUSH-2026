package com.example.physics

import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

data class GeneratedLevel(
    val points: List<Pair<Float, Float>>,
    val collectibles: MutableList<CollectibleItem>,
    val stars: MutableList<StarPickup>,
    val movingPlatforms: MutableList<MovingPlatform>,
    val ropeBridges: MutableList<RopeBridge>,
    val gates: MutableList<Gate>,
    val switches: MutableList<PressureSwitch>,
    val keys: MutableList<KeyPickup>,
    val spikes: MutableList<SpikeTrap>,
    val finishLineX: Float,
    // 20 NEW ADVANCED OBSTACLES & PUZZLES
    val speedBoostPads: MutableList<SpeedBoostPad> = mutableListOf(),
    val launchSprings: MutableList<LaunchSpring> = mutableListOf(),
    val seesawBridges: MutableList<SeesawBridge> = mutableListOf(),
    val collapsingBridges: MutableList<CollapsingBridge> = mutableListOf(),
    val rollingBoulders: MutableList<RollingBoulder> = mutableListOf(),
    val laserGrids: MutableList<LaserGrid> = mutableListOf(),
    val pendulumBlades: MutableList<PendulumBlade> = mutableListOf(),
    val windTunnels: MutableList<WindTunnel> = mutableListOf(),
    val explosiveBarrels: MutableList<ExplosiveBarrel> = mutableListOf(),
    val secretPaths: MutableList<SecretPath> = mutableListOf(),
    val pushableCrates: MutableList<PushableCrate> = mutableListOf(),
    val mudPits: MutableList<MudPit> = mutableListOf(),
    val icePatches: MutableList<IcePatch> = mutableListOf(),
    val conveyorBelts: MutableList<ConveyorBelt> = mutableListOf(),
    val bouncyMushrooms: MutableList<BouncyMushroom> = mutableListOf(),
    val fallingStalactites: MutableList<FallingStalactite> = mutableListOf(),
    val mysteryChests: MutableList<MysteryChest> = mutableListOf()
) {
    fun getSurfaceHeight(x: Float): Float {
        if (points.isEmpty()) return 0f
        if (x <= points.first().first) return points.first().second
        if (x >= points.last().first) return points.last().second

        var low = 0
        var high = points.size - 2
        while (low <= high) {
            val mid = (low + high) ushr 1
            val p0 = points[mid]
            val p1 = points[mid + 1]
            if (x >= p0.first && x <= p1.first) {
                val dx = p1.first - p0.first
                if (dx <= 0.0001f) return p0.second
                val t = (x - p0.first) / dx
                return p0.second + (p1.second - p0.second) * t
            } else if (x < p0.first) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return points.last().second
    }

    fun getSurfaceSlope(x: Float): Float {
        val h1 = getSurfaceHeight(x - 0.4f)
        val h2 = getSurfaceHeight(x + 0.4f)
        return (h2 - h1) / 0.8f
    }
}

class TerrainGenerator {
    fun generate(level: LevelInfo): GeneratedLevel {
        val points = mutableListOf<Pair<Float, Float>>()
        val collectibles = mutableListOf<CollectibleItem>()
        val stars = mutableListOf<StarPickup>()
        val movingPlatforms = mutableListOf<MovingPlatform>()
        val ropeBridges = mutableListOf<RopeBridge>()
        val gates = mutableListOf<Gate>()
        val switches = mutableListOf<PressureSwitch>()
        val keys = mutableListOf<KeyPickup>()
        val spikes = mutableListOf<SpikeTrap>()

        val speedBoostPads = mutableListOf<SpeedBoostPad>()
        val launchSprings = mutableListOf<LaunchSpring>()
        val seesawBridges = mutableListOf<SeesawBridge>()
        val collapsingBridges = mutableListOf<CollapsingBridge>()
        val rollingBoulders = mutableListOf<RollingBoulder>()
        val laserGrids = mutableListOf<LaserGrid>()
        val pendulumBlades = mutableListOf<PendulumBlade>()
        val windTunnels = mutableListOf<WindTunnel>()
        val explosiveBarrels = mutableListOf<ExplosiveBarrel>()
        val secretPaths = mutableListOf<SecretPath>()
        val pushableCrates = mutableListOf<PushableCrate>()
        val mudPits = mutableListOf<MudPit>()
        val icePatches = mutableListOf<IcePatch>()
        val conveyorBelts = mutableListOf<ConveyorBelt>()
        val bouncyMushrooms = mutableListOf<BouncyMushroom>()
        val fallingStalactites = mutableListOf<FallingStalactite>()
        val mysteryChests = mutableListOf<MysteryChest>()

        val step = 1.0f
        val totalLength = level.trackLengthMeters + 45f
        var x = -20f

        val seed = (level.globalLevelIndex * 31337).toLong()
        val random = Random(seed)

        // Baseline smooth procedural hills
        while (x <= totalLength) {
            val y = calculateHeight(x, level, random)
            points.add(Pair(x, y))
            x += step
        }

        // 3 Golden Stars (One placed on a secret / tricky route!)
        stars.add(StarPickup(index = 1, x = level.trackLengthMeters * 0.22f, y = calculateHeight(level.trackLengthMeters * 0.22f, level, random) - 2.8f))
        stars.add(StarPickup(index = 2, x = level.trackLengthMeters * 0.58f, y = calculateHeight(level.trackLengthMeters * 0.58f, level, random) - 3.8f))
        // Star 3 hidden in secret high path or chest area
        stars.add(StarPickup(index = 3, x = level.trackLengthMeters * 0.88f, y = calculateHeight(level.trackLengthMeters * 0.88f, level, random) - 4.5f))

        // Collectibles: Coins, Fuel, Diamonds, Nitro, and Magnets
        var fuelDist = 110f
        while (fuelDist < level.trackLengthMeters) {
            val fy = calculateHeight(fuelDist, level, random) - 1.2f
            collectibles.add(CollectibleItem(id = "fuel_$fuelDist", x = fuelDist, y = fy, type = CollectibleType.FUEL))
            fuelDist += 130f
        }

        var nitroDist = 70f
        while (nitroDist < level.trackLengthMeters) {
            val ny = calculateHeight(nitroDist, level, random) - 1.2f
            collectibles.add(CollectibleItem(id = "nitro_$nitroDist", x = nitroDist, y = ny, type = CollectibleType.NITRO))
            nitroDist += 150f
        }

        // Rare Magnet Pickup per level (attracts all coins)
        val magnetX = level.trackLengthMeters * 0.38f
        val magnetY = calculateHeight(magnetX, level, random) - 1.8f
        collectibles.add(CollectibleItem(id = "magnet_1", x = magnetX, y = magnetY, type = CollectibleType.MAGNET))

        // Coins and Diamonds with varied arcs
        var coinX = 20f
        while (coinX < level.trackLengthMeters) {
            val isHighArc = (coinX.toInt() % 65) < 22
            val countInArc = 5
            for (c in 0 until countInArc) {
                val cx = coinX + (c * 2.4f)
                if (cx >= level.trackLengthMeters) break
                val groundY = calculateHeight(cx, level, random)
                val cy = if (isHighArc) {
                    val archOffset = sin((c.toFloat() / (countInArc - 1)) * Math.PI.toFloat()) * 3.2f
                    groundY - 1.3f - archOffset
                } else {
                    groundY - 1.2f
                }
                collectibles.add(CollectibleItem(id = "coin_$cx", x = cx, y = cy, type = CollectibleType.COIN, value = 15))
            }

            if (coinX.toInt() % 130 == 0) {
                val dx = coinX + 14f
                val dy = calculateHeight(dx, level, random) - 2.8f
                collectibles.add(CollectibleItem(id = "dia_$dx", x = dx, y = dy, type = CollectibleType.DIAMOND, value = 1))
            }
            coinX += 28f
        }

        // Secret Path & Hidden Mystery Chest
        val secretStartX = level.trackLengthMeters * 0.62f
        val secretEndX = secretStartX + 35f
        val secretPoints = (0..14).map { i ->
            val px = secretStartX + (i * 2.5f)
            val py = calculateHeight(px, level, random) - 6.5f // upper sky rail!
            Pair(px, py)
        }
        secretPaths.add(SecretPath(id = "secret_1", startX = secretStartX, endX = secretEndX, points = secretPoints))
        mysteryChests.add(MysteryChest(x = secretStartX + 18f, y = calculateHeight(secretStartX + 18f, level, random) - 7.5f))

        // Speed Boost Pads
        val bpadX = level.trackLengthMeters * 0.18f
        speedBoostPads.add(SpeedBoostPad(x = bpadX, y = calculateHeight(bpadX, level, random)))

        // Launch Spring before big chasm
        val springX = level.trackLengthMeters * 0.42f
        launchSprings.add(LaunchSpring(x = springX, y = calculateHeight(springX, level, random) - 0.4f))

        // Seesaw Balance Bridge
        val seesawX = level.trackLengthMeters * 0.50f
        seesawBridges.add(SeesawBridge(pivotX = seesawX, pivotY = calculateHeight(seesawX, level, random) - 1.0f))

        // Collapsing Bridge
        val cBridgeX = level.trackLengthMeters * 0.70f
        collapsingBridges.add(CollapsingBridge(startX = cBridgeX, endX = cBridgeX + 18f, y = calculateHeight(cBridgeX, level, random)))

        // Rolling Boulder (rolls down high hill)
        val boulderX = level.trackLengthMeters * 0.32f
        rollingBoulders.add(RollingBoulder(x = boulderX + 25f, y = calculateHeight(boulderX + 25f, level, random) - 2f))

        // Laser Grid
        val laserX = level.trackLengthMeters * 0.78f
        laserGrids.add(LaserGrid(x = laserX, y = calculateHeight(laserX, level, random)))

        // Pendulum Blade over a ravine
        val pendX = level.trackLengthMeters * 0.82f
        pendulumBlades.add(PendulumBlade(pivotX = pendX, pivotY = calculateHeight(pendX, level, random) - 7.0f))

        // Wind Tunnel (Updraft)
        val windX = level.trackLengthMeters * 0.85f
        windTunnels.add(WindTunnel(startX = windX, endX = windX + 15f, startY = calculateHeight(windX, level, random) - 8f, endY = calculateHeight(windX, level, random)))

        // Explosive Barrels
        val barrelX = level.trackLengthMeters * 0.28f
        explosiveBarrels.add(ExplosiveBarrel(x = barrelX, y = calculateHeight(barrelX, level, random) - 0.9f))

        // Pushable Crate & Switch Puzzle
        val crateX = level.trackLengthMeters * 0.46f
        pushableCrates.add(PushableCrate(id = "crate_1", x = crateX, y = calculateHeight(crateX, level, random) - 1.5f))

        // Mud Pits & Ice Patches based on world
        val mudX = level.trackLengthMeters * 0.14f
        mudPits.add(MudPit(x = mudX, y = calculateHeight(mudX, level, random)))

        val iceX = level.trackLengthMeters * 0.36f
        icePatches.add(IcePatch(x = iceX, y = calculateHeight(iceX, level, random)))

        // Conveyor Belt
        val convX = level.trackLengthMeters * 0.65f
        conveyorBelts.add(ConveyorBelt(x = convX, y = calculateHeight(convX, level, random), width = 12f, beltSpeed = 6f))

        // Bouncy Mushrooms
        val mushX = level.trackLengthMeters * 0.54f
        bouncyMushrooms.add(BouncyMushroom(x = mushX, y = calculateHeight(mushX, level, random) - 0.5f))

        // Falling Stalactites
        val stalX = level.trackLengthMeters * 0.74f
        val sGround = calculateHeight(stalX, level, random)
        fallingStalactites.add(FallingStalactite(x = stalX, currentY = sGround - 8f, groundY = sGround))

        // Bridges & Moving Platforms
        if (level.hasBridges) {
            val bridgeStart = level.trackLengthMeters * 0.24f
            val by = calculateHeight(bridgeStart, level, random) + 0.8f
            val segs = (0..10).map { i ->
                val px = bridgeStart + (i * 2.2f)
                RopeBridgeSegment(x = px, y = by, targetY = by)
            }
            ropeBridges.add(RopeBridge(startX = bridgeStart, endX = bridgeStart + 22f, y = by, segments = segs))
        }

        if (level.hasMovingPlatforms) {
            val platX = level.trackLengthMeters * 0.56f
            val platY = calculateHeight(platX, level, random) - 2.2f
            movingPlatforms.add(
                MovingPlatform(
                    id = "mp_1",
                    startX = platX - 10f,
                    endX = platX + 10f,
                    startY = platY,
                    endY = platY,
                    width = 6.0f,
                    speed = 3.6f
                )
            )
        }

        // Spike Traps
        if (level.hasSpikeTraps) {
            val spkX = level.trackLengthMeters * 0.76f
            val spkY = calculateHeight(spkX, level, random)
            spikes.add(SpikeTrap(x = spkX, y = spkY, width = 6.0f, height = 1.0f))
        }

        // Gate & Switch Puzzle
        if (level.hasGatePuzzle) {
            val gateX = level.trackLengthMeters * 0.48f
            val gateY = calculateHeight(gateX, level, random)
            val switchX = gateX - 22f
            val switchY = calculateHeight(switchX, level, random)

            switches.add(PressureSwitch(id = "sw_1", x = switchX, y = switchY))
            gates.add(Gate(id = "gate_1", x = gateX, y = gateY, switchId = "sw_1"))
        }

        return GeneratedLevel(
            points = points,
            collectibles = collectibles,
            stars = stars,
            movingPlatforms = movingPlatforms,
            ropeBridges = ropeBridges,
            gates = gates,
            switches = switches,
            keys = keys,
            spikes = spikes,
            finishLineX = level.trackLengthMeters,
            speedBoostPads = speedBoostPads,
            launchSprings = launchSprings,
            seesawBridges = seesawBridges,
            collapsingBridges = collapsingBridges,
            rollingBoulders = rollingBoulders,
            laserGrids = laserGrids,
            pendulumBlades = pendulumBlades,
            windTunnels = windTunnels,
            explosiveBarrels = explosiveBarrels,
            secretPaths = secretPaths,
            pushableCrates = pushableCrates,
            mudPits = mudPits,
            icePatches = icePatches,
            conveyorBelts = conveyorBelts,
            bouncyMushrooms = bouncyMushrooms,
            fallingStalactites = fallingStalactites,
            mysteryChests = mysteryChests
        )
    }

    private fun calculateHeight(x: Float, level: LevelInfo, random: Random): Float {
        if (x < 15f) return 15f

        val freq = level.hillFrequency
        val amp = level.hillAmplitude

        val h1 = sin(x * freq) * amp
        val h2 = sin(x * freq * 2.3f + 1.2f) * (amp * 0.35f)
        val h3 = cos(x * freq * 0.45f) * (amp * 0.6f)
        val bumps = sin(x * 0.35f) * 0.8f

        return 20f + h1 + h2 + h3 + bumps
    }
}
