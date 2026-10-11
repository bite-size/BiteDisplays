package gg.bitesize.displays.listeners

import gg.bitesize.displays.managers.EntityManager
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent

/**
 * Keeps NPCs from being hurt. They're spawned invulnerable, but vanilla still lets creative players,
 * /kill, and the void through that, so every hit is cancelled.
 */
object ProtectionListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDamage(event: EntityDamageEvent) {
        if (isNpc(event.entity)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCombust(event: EntityCombustEvent) {
        if (isNpc(event.entity)) event.isCancelled = true
    }

    private fun isNpc(entity: Entity): Boolean {
        return entity is Mannequin && EntityManager.findByBukkitId(entity.uniqueId) != null
    }

}
