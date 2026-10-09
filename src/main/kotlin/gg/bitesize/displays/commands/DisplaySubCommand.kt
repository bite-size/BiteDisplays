package gg.bitesize.displays.commands

import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteDisplay
import gg.bitesize.displays.models.BiteType
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Locale

/**
 * Item and block displays: created from the held item, then rotated, scaled,
 * named, given a click command, and made to glow.
 */
abstract class DisplaySubCommand<T : BiteDisplay>(
    type: BiteType,
    permission: String,
    extraActions: List<String> = emptyList(),
) : EntitySubCommand<T>(type, permission, listOf("create", "rotate", "size", "name", "cmd", "glowing") + extraActions) {

    private val axes = listOf("yaw", "pitch")

    /** Builds a display from the held item, or returns null after messaging the player. */
    protected abstract fun create(player: Player, held: ItemStack): T?

    /** Actions beyond the shared ones. [args] excludes the action. */
    protected open fun execute(player: Player, display: T, action: String, label: String, args: List<String>) {}

    final override fun execute(player: Player, action: String, label: String, args: List<String>) {
        if (action == "create") {
            create(player, player.inventory.itemInMainHand)?.let { spawnAndSelect(player, it) }
            return
        }

        val display = selected(player, label) ?: return

        when (action) {
            "rotate" -> rotate(player, display, label, args)
            "size" -> size(player, display, label, args)
            "name" -> name(player, display, args)
            "cmd" -> command(player, display, args)
            "glowing" -> glowing(player, display, label, args)
            else -> execute(player, display, action, label, args)
        }
    }

    private fun rotate(player: Player, display: T, label: String, args: List<String>) {
        val axis = args.getOrNull(0)?.lowercase(Locale.ROOT)
        if (axis !in axes || args.size < 2) return sendActionUsage(player, label, "rotate")

        val degrees = args[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return error(player, "invalid-number")

        if (axis == "yaw") display.yaw = degrees else display.pitch = degrees
        EntityManager.updateEntity(display)
        success(player, "rotated",
            Placeholder.unparsed("axis", axis!!), Placeholder.unparsed("degrees", number(degrees)))
    }

    private fun size(player: Player, display: T, label: String, args: List<String>) {
        val scale = parseSize(player, label, "size", args) ?: return

        display.scale = scale
        EntityManager.updateEntity(display)
        sizeSet(player, scale)
    }

    private fun name(player: Player, display: T, args: List<String>) {
        if (args.isEmpty()) {
            display.nameText = null
            EntityManager.updateEntity(display)
            success(player, "name-removed")
            return
        }

        val name = args.joinToString(" ")
        display.nameText = name
        EntityManager.updateEntity(display)
        success(player, "name-set", Placeholder.parsed("name", name))
    }

    private fun command(player: Player, display: T, args: List<String>) {
        if (args.isEmpty()) {
            display.command = null
            EntityManager.updateEntity(display)
            success(player, "command-removed")
            return
        }

        val command = args.joinToString(" ")
        display.command = command
        EntityManager.updateEntity(display)
        success(player, "command-set", Placeholder.unparsed("command", command))
    }

    private fun glowing(player: Player, display: T, label: String, args: List<String>) {
        val glowing = parseToggle(player, label, "glowing", args) ?: return

        display.glowing = glowing
        EntityManager.updateEntity(display)
        success(player, if (glowing) "glowing-on" else "glowing-off")
    }

    override fun tabComplete(sender: CommandSender, action: String, args: List<String>): List<String> {
        return when (action) {
            "rotate" -> when (args.size) {
                1 -> matching(args[0], axes)
                2 -> hint(args[1], "<degrees>")
                else -> emptyList()
            }
            "size" -> if (args.size == 1) hint(args[0], "<scale>") else emptyList()
            "name" -> if (args.size == 1) hint(args[0], "[name]") else emptyList()
            "cmd" -> if (args.size == 1) hint(args[0], "[command]") else emptyList()
            "glowing" -> toggleCompletions(args)
            else -> emptyList()
        }
    }

}
