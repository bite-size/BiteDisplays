package gg.bitesize.displays.models

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class BiteNpc(
    override val id: UUID,
    override var location: Location,
    var name: String? = null,
    var skinValue: String? = null,
    var skinSignature: String? = null,
    var command: String? = null,
    var glowColor: String? = null,
    var description: String? = null,
    var sound: String? = null,
    var itemInHand: ItemStack? = null,
    var offHand: ItemStack? = null,
    var helmet: ItemStack? = null,
    var chestplate: ItemStack? = null,
    var leggings: ItemStack? = null,
    var boots: ItemStack? = null
) : BiteEntity {
    override val type: BiteType get() = BiteType.NPC
}
