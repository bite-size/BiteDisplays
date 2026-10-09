package gg.bitesize.displays.models

import org.bukkit.Location
import org.joml.Vector3f
import java.util.UUID

data class BiteText(
    override val id: UUID,
    override var location: Location,
    var lines: MutableList<String> = mutableListOf(),
    var scale: Vector3f = Vector3f(1f, 1f, 1f),
    var backgroundColor: String? = null,
    var glowColor: String? = null,
    var billboard: Boolean = false
) : BiteEntity {
    override val type: BiteType get() = BiteType.TEXT
}
