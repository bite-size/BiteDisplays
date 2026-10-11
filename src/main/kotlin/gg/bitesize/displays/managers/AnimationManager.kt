package gg.bitesize.displays.managers

import gg.bitesize.displays.BiteDisplays.Companion.INSTANCE
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteItem
import org.bukkit.entity.ItemDisplay
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.Vector3f
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

/**
 * Bounces item displays with /bd bouncing true.
 *
 * Each item gets a new height [KEYFRAMES] times per bounce, and players' games smooth the motion in between
 * (display interpolation), so the server sends one update where it used to send several. Only items that are
 * bouncing are visited, and ones nobody is tracking are skipped. Entity changes have to happen on the main
 * thread, so this runs there, kept to a few map lookups per item.
 */
object AnimationManager {

    private const val KEYFRAMES = 8

    private val bouncing = HashSet<UUID>()
    private var task: BukkitTask? = null
    private var keyframe = 0

    fun start() {
        val step = (ConfigManager.settings.bouncePeriod / KEYFRAMES).coerceAtLeast(1)
        task = INSTANCE.server.scheduler.runTaskTimer(INSTANCE, Runnable { tick(step.toInt()) }, 0L, step)
    }

    fun stop() {
        task?.cancel()
        task = null
        keyframe = 0
    }

    /** Picks up new animation settings after a reload. */
    fun restart() {
        stop()
        start()
    }

    /** Starts or stops animating [entity] to match its settings. Called whenever an entity is added or changed. */
    fun sync(entity: BiteEntity) {
        if (entity is BiteItem && entity.bouncing) bouncing.add(entity.id) else bouncing.remove(entity.id)
    }

    fun forget(id: UUID) {
        bouncing.remove(id)
    }

    fun clear() {
        bouncing.clear()
    }

    private fun tick(step: Int) {
        if (bouncing.isEmpty()) return

        keyframe = (keyframe + 1) % KEYFRAMES
        val yOffset = ConfigManager.settings.bounceHeight * sin(2 * PI * keyframe / KEYFRAMES).toFloat()

        for (id in bouncing) {
            val item = EntityManager.get(id) as? BiteItem ?: continue
            // Unloaded, or loaded where no player can see it
            val display = EntityManager.getBukkitEntity(id) as? ItemDisplay ?: continue
            if (display.trackedBy.isEmpty()) continue

            // From the item's own transformation, not read back from the entity
            val base = EntityManager.transformationOf(item)
            display.interpolationDelay = 0
            display.interpolationDuration = step
            display.transformation = Transformation(
                Vector3f(base.translation).add(0f, yOffset, 0f),
                base.leftRotation,
                base.scale,
                base.rightRotation,
            )
        }
    }
}
