package gg.bitesize.displays.api

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * A player right-clicked a BiteDisplays entity (an NPC, or a display's or text's hitbox). Fired before
 * the NPC's sound plays and the click command runs; cancel it to skip both, for example when your
 * plugin handles the click itself. Main thread. Clicks inside `clicks.cooldown` don't fire it.
 */
class BiteEntityClickEvent(
    val player: Player,
    val entity: BiteEntityInfo,
    /** What was clicked: the NPC itself, or a display's or text's hitbox. */
    val clicked: Entity,
) : Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * An entity is now in the world: just created, or respawned when its chunk loaded. Entities are
 * removed whenever their chunk unloads, so this fires again each time a player comes back.
 * Main thread.
 */
class BiteEntitySpawnEvent(
    val entity: BiteEntityInfo,
    /** The NPC (Mannequin) or display entity. */
    val bukkitEntity: Entity,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * A spawned entity was moved or edited (`/bd movehere`, `/bd setlocation`, size, name, and so on),
 * so anything placed next to it may need to move too. Main thread.
 */
class BiteEntityChangeEvent(
    val entity: BiteEntityInfo,
    val bukkitEntity: Entity,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}

/**
 * An entity left the world. [removed] is true when it was deleted (`/bd remove`), and false when its
 * chunk unloaded or it moved into an unloaded chunk (it spawns again later). Not fired on shutdown.
 * Main thread.
 */
class BiteEntityDespawnEvent(
    val entity: BiteEntityInfo,
    val removed: Boolean,
) : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
