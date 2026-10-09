package gg.bitesize.displays.gui

import gg.bitesize.displays.listeners.GuiListener
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.FormatManager
import gg.bitesize.displays.models.BiteNpc
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

    // CraftBukkit maps a leading reset to an explicitly non-italic name
    private const val UPRIGHT = "§r"

    fun open(player: Player, npc: BiteNpc) {
        val title = FormatManager.toLegacy(gui("title"))
        val inv = Bukkit.createInventory(null, 36, title)

        // Fill with glass panes
        val glass = createGlassPane(" ", Material.BLACK_STAINED_GLASS_PANE)
        for (i in 0 until 36) {
            if (i !in EQUIPMENT_SLOTS) {
                inv.setItem(i, glass)
            }
        }

        // Labels adjacent to equipment slots
        inv.setItem(3, label("helmet", Material.LIGHT_BLUE_STAINED_GLASS_PANE, pointsRight = true))
        inv.setItem(11, label("main-hand", Material.LIME_STAINED_GLASS_PANE, pointsRight = true))
        inv.setItem(15, label("off-hand", Material.LIME_STAINED_GLASS_PANE, pointsRight = false))
        inv.setItem(21, label("leggings", Material.LIGHT_BLUE_STAINED_GLASS_PANE, pointsRight = true))
        inv.setItem(30, label("boots", Material.LIGHT_BLUE_STAINED_GLASS_PANE, pointsRight = true))
        inv.setItem(5, label("helmet", Material.LIGHT_BLUE_STAINED_GLASS_PANE, pointsRight = false))
        inv.setItem(23, label("leggings", Material.LIGHT_BLUE_STAINED_GLASS_PANE, pointsRight = false))
        inv.setItem(32, label("boots", Material.LIGHT_BLUE_STAINED_GLASS_PANE, pointsRight = false))

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

    private fun gui(key: String): String = ConfigManager.getMessage("gui.equipment-$key")

    private fun label(key: String, material: Material, pointsRight: Boolean): ItemStack {
        val text = gui(key)
        return createGlassPane(if (pointsRight) "$text →" else "← $text", material)
    }

    private fun createGlassPane(name: String, material: Material): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(UPRIGHT + FormatManager.toLegacy(name))
        item.itemMeta = meta
        return item
    }
}
