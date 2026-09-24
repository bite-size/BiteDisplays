package gg.bitesize.plugin.gui

import gg.bitesize.plugin.listeners.GuiListener
import gg.bitesize.plugin.managers.FormatManager
import gg.bitesize.plugin.models.BiteNpc
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Equipment GUI for NPC entities.
 * Opens a 36-slot chest inventory with labeled equipment slots.
 *
 * Layout (4 rows):
 *   Row 0: [·] [·] [·] [·] [Helmet]     [·]  [·] [·] [·]
 *   Row 1: [·] [·] [·] [MainHand] [Chestplate] [OffHand] [·] [·] [·]
 *   Row 2: [·] [·] [·] [·] [Leggings]   [·]  [·] [·] [·]
 *   Row 3: [·] [·] [·] [·] [Boots]      [·]  [·] [·] [·]
 */
object EquipmentGui {

    const val HELMET_SLOT = 4
    const val MAINHAND_SLOT = 12
    const val CHESTPLATE_SLOT = 13
    const val OFFHAND_SLOT = 14
    const val LEGGINGS_SLOT = 22
    const val BOOTS_SLOT = 31

    val EQUIPMENT_SLOTS = setOf(HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, MAINHAND_SLOT, OFFHAND_SLOT)

    fun open(player: Player, npc: BiteNpc) {
        val title = FormatManager.parseMiniMessage("<gradient:#F0A6CA:#99E1D9>NPC Equipment</gradient>")
        val inv = Bukkit.createInventory(null, 36, title)

        // Fill with glass panes
        val glass = createGlassPane(" ", Material.BLACK_STAINED_GLASS_PANE)
        for (i in 0 until 36) {
            if (i !in EQUIPMENT_SLOTS) {
                inv.setItem(i, glass)
            }
        }

        // Labels adjacent to equipment slots
        inv.setItem(3, createGlassPane("Helmet →", Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        inv.setItem(11, createGlassPane("Main Hand →", Material.LIME_STAINED_GLASS_PANE))
        inv.setItem(15, createGlassPane("← Off Hand", Material.LIME_STAINED_GLASS_PANE))
        inv.setItem(21, createGlassPane("Leggings →", Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        inv.setItem(30, createGlassPane("Boots →", Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        inv.setItem(5, createGlassPane("← Helmet", Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        inv.setItem(23, createGlassPane("← Leggings", Material.LIGHT_BLUE_STAINED_GLASS_PANE))
        inv.setItem(32, createGlassPane("← Boots", Material.LIGHT_BLUE_STAINED_GLASS_PANE))

        // Place current equipment
        npc.helmet?.let { inv.setItem(HELMET_SLOT, it.clone()) }
        npc.chestplate?.let { inv.setItem(CHESTPLATE_SLOT, it.clone()) }
        npc.leggings?.let { inv.setItem(LEGGINGS_SLOT, it.clone()) }
        npc.boots?.let { inv.setItem(BOOTS_SLOT, it.clone()) }
        npc.itemInHand?.let { inv.setItem(MAINHAND_SLOT, it.clone()) }
        npc.offHand?.let { inv.setItem(OFFHAND_SLOT, it.clone()) }

        // Track this GUI session
        GuiListener.openGuis[player.uniqueId] = npc.id

        player.openInventory(inv)
    }

    private fun createGlassPane(name: String, material: Material): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(name).color(FormatManager.WHITE))
        item.itemMeta = meta
        return item
    }
}
