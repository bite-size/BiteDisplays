package gg.bitesize.plugin.listeners

import gg.bitesize.plugin.managers.EntityManager
import gg.bitesize.plugin.models.BiteBlock
import gg.bitesize.plugin.models.BiteItem
import gg.bitesize.plugin.models.BiteNpc
import org.bukkit.Bukkit
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Handles players right-clicking tracked entities and companion Interaction hitboxes.
 * Executes click commands with {player} placeholder and plays NPC sounds.
 */
class EntityInteractListener : Listener {

    /**
     * Primary handler for entity interactions.
     * Catches Mannequins, Display entities, and Interaction entities (if routed here).
     */
    @EventHandler
    fun onInteract(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        // Check main tracked entities (NPCs, displays)
        val biteEntity = EntityManager.findByBukkitId(event.rightClicked.uniqueId)
        if (biteEntity != null) {
            event.isCancelled = true

            if (biteEntity is BiteNpc) {
                // Play click sound (string-based playSound for registry-backed sounds)
                biteEntity.sound?.let { soundKey ->
                    event.player.playSound(biteEntity.location, "minecraft:$soundKey", 1.0f, 1.0f)
                }

                // Execute click command
                biteEntity.command?.let { cmd ->
                    val resolved = cmd.replace("{player}", event.player.name)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved)
                }
            }
            return
        }

        // Check companion Interaction entities (item/block cmd hitboxes)
        val parentEntity = EntityManager.findByCompanionBukkitId(event.rightClicked.uniqueId)
        if (parentEntity != null) {
            event.isCancelled = true
            executeCompanionCommand(event.player, parentEntity)
        }
    }

    /**
     * Backup handler specifically for Interaction entities,
     * which may only fire InteractAt (not the parent event).
     * Ignores non-Interaction entities to prevent Mannequin double-firing.
     */
    @EventHandler
    fun onInteractAt(event: PlayerInteractAtEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.isCancelled) return
        if (event.rightClicked !is Interaction) return

        val parentEntity = EntityManager.findByCompanionBukkitId(event.rightClicked.uniqueId)
        if (parentEntity != null) {
            event.isCancelled = true
            executeCompanionCommand(event.player, parentEntity)
        }
    }

    private fun executeCompanionCommand(player: Player, entity: gg.bitesize.plugin.models.BiteEntity) {
        val command = when (entity) {
            is BiteItem -> entity.command
            is BiteBlock -> entity.command
            else -> null
        } ?: return

        val resolved = command.replace("{player}", player.name)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved)
    }
}
