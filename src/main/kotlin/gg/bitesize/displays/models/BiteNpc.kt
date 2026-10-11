package gg.bitesize.displays.models

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class BiteNpc(
    override val id: UUID,
    override val number: Int,
    override var location: Location,
    var name: String? = null,
    var skinValue: String? = null,
    var skinSignature: String? = null,
    override var command: ClickCommand? = null,
    var glowColor: String? = null,
    /** Null hides the line below the name. */
    var description: String? = null,
    var sound: String? = null,
    /** The scale attribute, from [MIN_SCALE] to [MAX_SCALE]. */
    var scale: Double = 1.0,
    var itemInHand: ItemStack? = null,
    var offHand: ItemStack? = null,
    var helmet: ItemStack? = null,
    var chestplate: ItemStack? = null,
    var leggings: ItemStack? = null,
    var boots: ItemStack? = null
) : Clickable {
    override val type: BiteType get() = BiteType.NPC

    companion object {
        // Vanilla's bounds for the scale attribute
        const val MIN_SCALE = 0.0625
        const val MAX_SCALE = 16.0
    }
}
