package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.EntitySubCommand
import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.util.Colors
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class TextDisplaySubCommand : EntitySubCommand<BiteText>(
    BiteType.TEXT, Permissions.TEXT,
    listOf("create", "background", "addline", "delline", "editline", "glow", "billboard", "size"),
) {

    override fun execute(player: Player, action: String, label: String, args: List<String>) {
        if (action == "create") return create(player, args)

        val text = selected(player, label) ?: return

        when (action) {
            "background" -> background(player, text, label, args)
            "addline" -> addLine(player, text, label, args)
            "delline" -> deleteLine(player, text, label, args)
            "editline" -> editLine(player, text, label, args)
            // Displays glow in any color, named or hex
            "glow" -> glow(player, args, { Colors.parse(it) != null }, "invalid-glow-color") {
                text.glowColor = it
                EntityManager.updateEntity(text)
            }
            "billboard" -> billboard(player, text, label, args)
            "size" -> size(player, text, label, args)
        }
    }

    private fun create(player: Player, args: List<String>) {
        val firstLine = if (args.isEmpty()) settings.defaultTextLine else args.joinToString(" ")

        spawnAndSelect(player, BiteText(id = UUID.randomUUID(), number = nextNumber(), location = spawnLocation(player),
            lines = mutableListOf(firstLine), billboard = settings.defaultTextBillboard))
    }

    private fun background(player: Player, text: BiteText, label: String, args: List<String>) {
        val color = args.firstOrNull() ?: return sendActionUsage(player, label, "background")
        if (Colors.parseHex(color) == null) return error(player, "invalid-color")

        text.backgroundColor = color
        EntityManager.updateEntity(text)
        success(player, "background-set", Placeholder.unparsed("color", color))
    }

    private fun addLine(player: Player, text: BiteText, label: String, args: List<String>) {
        if (args.isEmpty()) return sendActionUsage(player, label, "addline")

        text.lines.add(args.joinToString(" "))
        EntityManager.updateEntity(text)
        success(player, "line-added", linePlaceholder(text.lines.size))
    }

    private fun deleteLine(player: Player, text: BiteText, label: String, args: List<String>) {
        val line = args.firstOrNull() ?: return sendActionUsage(player, label, "delline")
        val index = lineIndex(player, text, line) ?: return

        text.lines.removeAt(index)
        EntityManager.updateEntity(text)
        success(player, "line-removed", linePlaceholder(index + 1))
    }

    private fun editLine(player: Player, text: BiteText, label: String, args: List<String>) {
        if (args.size < 2) return sendActionUsage(player, label, "editline")
        val index = lineIndex(player, text, args[0]) ?: return

        text.lines[index] = args.drop(1).joinToString(" ")
        EntityManager.updateEntity(text)
        success(player, "line-edited", linePlaceholder(index + 1))
    }

    private fun billboard(player: Player, text: BiteText, label: String, args: List<String>) {
        val billboard = parseToggle(player, label, "billboard", args) ?: return

        text.billboard = billboard
        EntityManager.updateEntity(text)
        success(player, if (billboard) "billboard-on" else "billboard-off")
    }

    private fun size(player: Player, text: BiteText, label: String, args: List<String>) {
        val scale = parseSize(player, label, "size", args) ?: return

        text.scale = scale
        EntityManager.updateEntity(text)
        sizeSet(player, scale)
    }

    /** The 0-based index of a 1-based line number, or null after messaging the player. */
    private fun lineIndex(player: Player, text: BiteText, input: String): Int? {
        val line = input.toIntOrNull()
        if (line == null || line !in 1..text.lines.size) {
            error(player, "invalid-line", Placeholder.unparsed("lines", text.lines.size.toString()))
            return null
        }
        return line - 1
    }

    private fun linePlaceholder(line: Int) = Placeholder.unparsed("line", line.toString())

    override fun tabComplete(sender: CommandSender, action: String, args: List<String>): List<String> {
        val input = args.last()
        val lineNumbers = { (1..(selectedOrNull(sender)?.lines?.size ?: 0)).map(Int::toString) }

        return when (action) {
            "create" -> if (args.size == 1) hint(input, "[text]") else emptyList()
            "background" -> if (args.size == 1) hint(input, "<#AARRGGBB>") else emptyList()
            "addline" -> if (args.size == 1) hint(input, "<text>") else emptyList()
            "delline" -> if (args.size == 1) matching(input, lineNumbers()) else emptyList()
            "editline" -> when (args.size) {
                1 -> matching(input, lineNumbers())
                2 -> hint(input, "<text>")
                else -> emptyList()
            }
            "glow" -> glowCompletions(args)
            "billboard" -> toggleCompletions(args)
            "size" -> if (args.size == 1) hint(input, "<scale>") else emptyList()
            else -> emptyList()
        }
    }

}
