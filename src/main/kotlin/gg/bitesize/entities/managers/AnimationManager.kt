package gg.bitesize.entities.managers

import gg.bitesize.entities.BiteEntities.Companion.INSTANCE
import gg.bitesize.entities.models.BiteItem
import org.bukkit.entity.ItemDisplay
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

    fun start() {
        val interval = ConfigManager.settings.bounceInterval
        task = INSTANCE.server.scheduler.runTaskTimer(INSTANCE, Runnable {
            tickCount++
            for (entity in EntityManager.getAll()) {
                if (entity is BiteItem && entity.bouncing) {
                    animateBounce(entity, interval)
                }
            }
        }, 0L, interval)
    }

    fun stop() {
        task?.cancel()
        task = null
        tickCount = 0
    }

    /** Picks up new animation settings after a reload. */
    fun restart() {
        stop()
        start()
    }

    private fun animateBounce(item: BiteItem, interval: Long) {
        val display = EntityManager.getBukkitEntity(item.id) as? ItemDisplay ?: return
        val settings = ConfigManager.settings
        val yOffset = settings.bounceHeight * sin(tickCount.toFloat() * settings.bounceSpeed)
        val t = display.transformation
        display.interpolationDelay = 0
        display.interpolationDuration = interval.toInt()
        display.transformation = Transformation(
            Vector3f(t.translation.x, yOffset, t.translation.z),
            t.leftRotation,
            t.scale,
            t.rightRotation
        )
    }
}
