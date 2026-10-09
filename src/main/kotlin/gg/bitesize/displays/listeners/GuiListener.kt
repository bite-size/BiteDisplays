package gg.bitesize.displays.listeners

import gg.bitesize.displays.gui.EquipmentGui
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.managers.FormatManager
import gg.bitesize.displays.models.BiteNpc
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles inventory events for the NPC Equipment GUI.
 * Locks glass pane slots, and applies equipment to the NPC when the GUI is closed.
 */
object GuiListener : Listener {

    /** Maps player UUID → NPC entity UUID for open equipment GUIs. */
    val openGuis = ConcurrentHashMap<UUID, UUID>()

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val npcId = openGuis.remove(player.uniqueId) ?: return
        val npc = EntityManager.get(npcId) as? BiteNpc ?: return

        val inv = event.inventory

        // Read equipment from GUI slots
        npc.helmet = inv.getItem(EquipmentGui.HELMET_SLOT)?.takeUnless { isGlassPane(it) }
        npc.chestplate = inv.getItem(EquipmentGui.CHESTPLATE_SLOT)?.takeUnless { isGlassPane(it) }
        npc.leggings = inv.getItem(EquipmentGui.LEGGINGS_SLOT)?.takeUnless { isGlassPane(it) }
        npc.boots = inv.getItem(EquipmentGui.BOOTS_SLOT)?.takeUnless { isGlassPane(it) }
        npc.itemInHand = inv.getItem(EquipmentGui.MAINHAND_SLOT)?.takeUnless { isGlassPane(it) }
        npc.offHand = inv.getItem(EquipmentGui.OFFHAND_SLOT)?.takeUnless { isGlassPane(it) }

        EntityManager.updateEntity(npc)
        FormatManager.send(player, ConfigManager.getMessage("messages.equipment-updated"))
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!openGuis.containsKey(player.uniqueId)) return

        // Allow clicks in the player's own inventory
        if (event.clickedInventory != event.view.topInventory) return

        // In the GUI, only allow clicks on equipment slots
        if (event.rawSlot !in EquipmentGui.EQUIPMENT_SLOTS) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!openGuis.containsKey(player.uniqueId)) return

        // Cancel if any dragged slot is not an equipment slot
        if (event.rawSlots.any { it !in EquipmentGui.EQUIPMENT_SLOTS }) {
            event.isCancelled = true
        }
    }

    private fun isGlassPane(item: ItemStack): Boolean {
        return item.type.name.endsWith("_STAINED_GLASS_PANE")
    }
}
