package gg.bitesize.displays.api

import gg.bitesize.displays.BiteDisplays
import gg.bitesize.displays.managers.EntityManager
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox

/**
 * Read-only access to BiteDisplays entities for other plugins. Main thread only.
 *
 * Plugins that would rather not depend on BiteDisplays can read the [ID_KEY] tag instead: every
 * entity BiteDisplays spawns (and its name label and hitbox) carries its display ID there.
 */
object BiteDisplaysApi {

    /** `bitedisplays:id`, a string tag holding the display ID (`npc-3`) on every spawned entity. */
    @JvmStatic
    val ID_KEY: NamespacedKey by lazy { NamespacedKey(BiteDisplays.INSTANCE, "id") }

    /** The entity with this ID (`npc-3`, any case), spawned or not. */
    @JvmStatic
    fun find(displayId: String): BiteEntityInfo? = EntityManager.findByDisplayId(displayId)?.let(::BiteEntityInfo)

    /** The entity a Bukkit entity belongs to, whether it's the entity itself or its hitbox or name label. */
    @JvmStatic
    fun fromBukkit(entity: Entity): BiteEntityInfo? = EntityManager.findByBukkitId(entity.uniqueId)?.let(::BiteEntityInfo)

    /** Every entity, spawned or not. */
    @JvmStatic
    fun all(): List<BiteEntityInfo> = EntityManager.getAll().map(::BiteEntityInfo)

    /** The live NPC or display entity, or null while its chunk isn't loaded. */
    @JvmStatic
    fun bukkitEntity(displayId: String): Entity? {
        val entity = EntityManager.findByDisplayId(displayId) ?: return null
        return EntityManager.getBukkitEntity(entity.id)
    }

    @JvmStatic
    fun isSpawned(displayId: String): Boolean = bukkitEntity(displayId) != null

    /**
     * The space it appears to fill, in world coordinates (a copy), for placing things above or around
     * it. NPCs only have bounds while spawned; displays always do.
     */
    @JvmStatic
    fun bounds(displayId: String): BoundingBox? {
        val entity = EntityManager.findByDisplayId(displayId) ?: return null
        return EntityManager.boundsOf(entity)?.clone()
    }

    internal fun tag(entity: Entity, displayId: String) {
        entity.persistentDataContainer.set(ID_KEY, PersistentDataType.STRING, displayId)
    }

}
