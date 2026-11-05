package gg.bitesize.displays.api

import gg.bitesize.displays.models.BiteDisplay
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteNpc
import org.bukkit.Location

/**
 * A read-only look at one BiteDisplays entity, taken when it was asked for.
 * Plain types only, so it works the same on Spigot, Paper, and Purpur.
 */
class BiteEntityInfo internal constructor(entity: BiteEntity) {

    /** The ID players see and type, like `npc-3`. Never changes. */
    val displayId: String = entity.displayId

    /** `npc`, `item`, `block`, or `text`. */
    val type: String = entity.type.key

    private val where: Location = entity.location.clone()

    /** Where it stands. A copy, so changing it changes nothing. */
    val location: Location get() = where.clone()

    /**
     * The name as typed (MiniMessage or legacy codes) for NPCs and item and block displays,
     * or null when it has none. Text displays have lines instead, so this is null for them.
     */
    val name: String? = when (entity) {
        is BiteNpc -> entity.name
        is BiteDisplay -> entity.nameText
        else -> null
    }

    /** NPC size (the scale attribute), 1.0 for everything else. */
    val scale: Double = (entity as? BiteNpc)?.scale ?: 1.0

    val isNpc: Boolean get() = type == "npc"

    override fun toString(): String = "BiteEntityInfo($displayId)"

}
