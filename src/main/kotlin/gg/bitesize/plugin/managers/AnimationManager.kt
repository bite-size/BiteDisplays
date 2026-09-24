package gg.bitesize.plugin.managers

import gg.bitesize.plugin.models.BiteItem
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Vector3f
import kotlin.math.sin

/**
 * Manages repeating animations for display entities.
 * Currently handles the "bouncing" animation for BiteItem entities.
 */
object AnimationManager {

    private var task: BukkitTask? = null
    private var tickCount = 0L

    fun start(plugin: JavaPlugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            tickCount++
            for (entity in EntityManager.getAll()) {
                if (entity is BiteItem && entity.bouncing) {
                    animateBounce(entity)
                }
            }
        }, 0L, 4L) // Every 4 ticks (200ms)
    }

    fun stop() {
        task?.cancel()
        task = null
        tickCount = 0
    }

    private fun animateBounce(item: BiteItem) {
        val display = EntityManager.getBukkitEntity(item.id) as? ItemDisplay ?: return
        val yOffset = 0.12f * sin(tickCount.toFloat() * 0.15f)
        val t = display.transformation
        display.interpolationDelay = 0
        display.interpolationDuration = 4
        display.transformation = Transformation(
            Vector3f(t.translation.x, yOffset, t.translation.z),
            t.leftRotation,
            t.scale,
            t.rightRotation
        )
    }
}
