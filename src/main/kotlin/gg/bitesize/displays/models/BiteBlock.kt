package gg.bitesize.displays.models

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import java.util.UUID

data class BiteBlock(
    override val id: UUID,
    override var location: Location,
    var itemStack: ItemStack,
    override var scale: Vector3f = Vector3f(1f, 1f, 1f),
    override var yaw: Float = 0f,
    override var pitch: Float = 0f,
    override var nameText: String? = null,
    override var command: String? = null,
    override var glowing: Boolean = false,
    override var glowColor: String? = null
) : BiteDisplay {
    override val type: BiteType get() = BiteType.BLOCK
}
