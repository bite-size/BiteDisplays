package gg.bitesize.plugin.models

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import java.util.UUID

data class BiteItem(
    override val id: UUID,
    override var location: Location,
    var itemStack: ItemStack,
    var scale: Vector3f = Vector3f(1f, 1f, 1f),
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    var nameText: String? = null,
    var command: String? = null,
    var glowing: Boolean = false,
    var glowColor: String? = null,
    var bouncing: Boolean = false
) : BiteEntity
