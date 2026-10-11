package gg.bitesize.displays.models

import org.bukkit.Location
import org.joml.Vector3f
import java.util.Locale
import java.util.UUID

/** The four kinds of BiteEntity. [key] is used in config.yml, messages.yml, entities.json, and IDs. */
enum class BiteType(val key: String) {
    NPC("npc"),
    ITEM("item"),
    BLOCK("block"),
    TEXT("text"),
}

/** Who runs a click command. [key] is what players type and what entities.json stores. */
enum class CommandRunner(val key: String) {
    CONSOLE("console"),
    PLAYER("player");

    companion object {
        fun fromKey(key: String?): CommandRunner? = entries.firstOrNull { it.key == key?.lowercase(Locale.ROOT) }
    }
}

/** A command run when a player right-clicks an entity. */
data class ClickCommand(val runner: CommandRunner, val command: String)

interface BiteEntity {
    /** Internal key in memory and entities.json. Players see [displayId] instead. */
    val id: UUID
    /** Numbered per type, starting at 1. */
    val number: Int
    val location: Location
    val type: BiteType

    /** The ID players see and type, like npc-1. */
    val displayId: String get() = "${type.key}-$number"
}

/** An entity that runs a command when right-clicked: every type. */
interface Clickable : BiteEntity {
    var command: ClickCommand?
}

/** An item or block display: both can be rotated, scaled, named, clicked, and made to glow. */
interface BiteDisplay : Clickable {
    var scale: Vector3f
    var yaw: Float
    var pitch: Float
    var nameText: String?
    /** A color name or hex. Null means no glow. */
    var glowColor: String?
}
