package gg.bitesize.displays.models

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import java.util.UUID

data class BiteItem(
    override val id: UUID,
    override val number: Int,
    override var location: Location,
    var itemStack: ItemStack,
    override var scale: Vector3f = Vector3f(1f, 1f, 1f),
    override var yaw: Float = 0f,
    override var pitch: Float = 0f,
    override var nameText: String? = null,
    override var command: ClickCommand? = null,
    override var glowColor: String? = null,
    var bouncing: Boolean = false
) : BiteDisplay {
    override val type: BiteType get() = BiteType.ITEM
}
