package gg.bitesize.plugin.listeners

import gg.bitesize.plugin.managers.EntityManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

/**
 * Keeps tracked entities alive across chunk load/unload cycles.
 *
 * All BiteEntities are spawned with isPersistent = false, meaning Minecraft
 * won't save them. We handle persistence ourselves:
 *   - On chunk unload: drop the Bukkit entity handle (MC already despawns it).
 *   - On chunk load: respawn any BiteEntities whose locations fall in this chunk.
 */
class WorldListener : Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk

        for (entity in EntityManager.getAll()) {
            val loc = entity.location
            // Check if this entity belongs to the loading chunk
            if (loc.world?.uid != chunk.world.uid) continue
            if (loc.blockX shr 4 != chunk.x || loc.blockZ shr 4 != chunk.z) continue

            // If the Bukkit handle is gone or dead, respawn it
            val bukkit = EntityManager.getBukkitEntity(entity.id)
            if (bukkit == null || bukkit.isDead) {
                EntityManager.respawn(entity)
            }
        }
    }

    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        val chunk = event.chunk

        for (entity in EntityManager.getAll()) {
            val loc = entity.location
            if (loc.world?.uid != chunk.world.uid) continue
            if (loc.blockX shr 4 != chunk.x || loc.blockZ shr 4 != chunk.z) continue

            // Drop the Bukkit handle — MC is despawning the entity with the chunk.
            // The BiteEntity data stays in EntityManager for respawn on next load.
            EntityManager.removeBukkitEntity(entity.id)
        }
    }
}
