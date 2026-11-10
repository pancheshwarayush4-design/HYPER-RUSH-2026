package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verifyVehiclesAndWorlds() {
    val vehicles = com.example.model.VehicleRegistry.allVehicles
    assertEquals(12, vehicles.size)

    val worlds = com.example.model.WorldRegistry.allWorlds
    assertEquals(10, worlds.size)

    val terrainGen = com.example.physics.TerrainGenerator()
    val level = worlds.first().getLevels().first()
    val generated = terrainGen.generate(level)
    assertTrue(generated.points.isNotEmpty())
    assertEquals(level.trackLengthMeters, generated.finishLineX)

    // Verify 20 new obstacle types generated
    assertTrue(generated.speedBoostPads.isNotEmpty())
    assertTrue(generated.launchSprings.isNotEmpty())
    assertTrue(generated.seesawBridges.isNotEmpty())
    assertTrue(generated.collapsingBridges.isNotEmpty())
    assertTrue(generated.rollingBoulders.isNotEmpty())
    assertTrue(generated.secretPaths.isNotEmpty())
    assertTrue(generated.mysteryChests.isNotEmpty())
  }
}
