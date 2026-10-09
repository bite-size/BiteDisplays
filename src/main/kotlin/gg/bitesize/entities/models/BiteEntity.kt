package gg.bitesize.entities.models

import org.bukkit.Location
import org.joml.Vector3f
import java.util.UUID

/** The four kinds of BiteEntity. [key] is used in config.yml, messages.yml, and entities.json. */
enum class BiteType(val key: String) {
    NPC("npc"),
    ITEM("item"),
    BLOCK("block"),
    TEXT("text"),
}

interface BiteEntity {
    val id: UUID
    val location: Location
    val type: BiteType

    /** The first 8 characters of [id], shown to players. */
    val shortId: String get() = id.toString().take(8)
}

/** An item or block display: both can be rotated, scaled, named, clicked, and made to glow. */
interface BiteDisplay : BiteEntity {
    var scale: Vector3f
    var yaw: Float
    var pitch: Float
    var nameText: String?
    var command: String?
    var glowing: Boolean
    var glowColor: String?
}
