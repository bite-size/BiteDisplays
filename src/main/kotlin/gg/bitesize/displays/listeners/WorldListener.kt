package gg.bitesize.displays.listeners

import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.storage.EntityDatabase
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

/**
 * Keeps tracked entities alive across chunk load/unload cycles.
 *
 * All BiteDisplays are spawned with isPersistent = false, meaning Minecraft
 * won't save them. We handle persistence ourselves:
 *   - On chunk unload: remove the Bukkit entity and its companions, and drop the handles.
 *   - On chunk load: respawn any BiteDisplays whose locations fall in this chunk.
 *
 * Chunk events are the most frequent on a server, so each one is a single lookup in
 * EntityManager's chunk index, and chunks without our entities cost nothing more.
 *
 * Worlds can load after BiteDisplays (Multiverse and other world plugins) and unload while the server runs,
 * so EntityDatabase holds the entities of worlds that aren't loaded and restores them when the world loads.
 */
object WorldListener : Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        val chunk = event.chunk

        for (entity in EntityManager.inChunk(chunk.world, chunk.x, chunk.z)) {
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

        // Removed with the chunk, not left for the server to discard later (see unloadBukkitEntity).
        // The BiteEntity data stays in EntityManager for respawn on next load.
        for (entity in EntityManager.inChunk(chunk.world, chunk.x, chunk.z)) {
            EntityManager.unloadBukkitEntity(entity.id)
        }
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        EntityDatabase.onWorldLoad(event.world)
    }

    // Monitor, so a world another plugin keeps loaded (cancelled) keeps its entities
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWorldUnload(event: WorldUnloadEvent) {
        EntityDatabase.onWorldUnload(event.world)
    }
}
