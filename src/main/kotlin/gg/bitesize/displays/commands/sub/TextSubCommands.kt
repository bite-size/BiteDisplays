package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.EditSubCommand
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.util.Colors
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player

// Actions only text displays have

/** addline, editline, and delline. Lines are numbered from 1, as players see them. */
abstract class LineSubCommand(name: String) : EditSubCommand<BiteText>(name, BiteType.TEXT) {

    /** The 0-based index of a 1-based line number, or null after messaging the player. */
    protected fun lineIndex(player: Player, text: BiteText, input: String): Int? {
        val line = input.toIntOrNull()
        if (line == null || line !in 1..text.lines.size) {
            error(player, "invalid-line", Placeholder.unparsed("lines", text.lines.size.toString()))
            return null
        }
        return line - 1
    }

    protected fun lineNumbers(text: BiteText): List<String> = (1..text.lines.size).map(Int::toString)

    protected fun linePlaceholder(line: Int) = Placeholder.unparsed("line", line.toString())

}

class AddLineSubCommand : LineSubCommand("addline") {

    override fun edit(player: Player, entity: BiteText, label: String, args: List<String>) {
        if (args.isEmpty()) return sendUsage(player, label)

        entity.lines.add(args.joinToString(" "))
        EntityManager.updateEntity(entity)
        success(player, "line-added", linePlaceholder(entity.lines.size))
    }

    override fun complete(entity: BiteText, args: List<String>): List<String> {
        return if (args.size == 1) hint(args[0], "<text>") else emptyList()
    }

}

class EditLineSubCommand : LineSubCommand("editline") {

    override fun edit(player: Player, entity: BiteText, label: String, args: List<String>) {
        if (args.size < 2) return sendUsage(player, label)
        val index = lineIndex(player, entity, args[0]) ?: return

        entity.lines[index] = args.drop(1).joinToString(" ")
        EntityManager.updateEntity(entity)
        success(player, "line-edited", linePlaceholder(index + 1))
    }

    override fun complete(entity: BiteText, args: List<String>): List<String> = when (args.size) {
        1 -> matching(args[0], lineNumbers(entity))
        2 -> hint(args[1], "<text>")
        else -> emptyList()
    }

}

class DeleteLineSubCommand : LineSubCommand("delline") {

    override fun edit(player: Player, entity: BiteText, label: String, args: List<String>) {
        val line = args.firstOrNull() ?: return sendUsage(player, label)
        val index = lineIndex(player, entity, line) ?: return

        entity.lines.removeAt(index)
        EntityManager.updateEntity(entity)
        success(player, "line-removed", linePlaceholder(index + 1))
    }

    override fun complete(entity: BiteText, args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], lineNumbers(entity)) else emptyList()
    }

}

/** `background [hex]`: no color or `none` goes back to text.default-background, like a new display. */
class BackgroundSubCommand : EditSubCommand<BiteText>("background", BiteType.TEXT) {

    override fun edit(player: Player, entity: BiteText, label: String, args: List<String>) {
        val color = args.firstOrNull()
        if (color == null || color.equals(NO_BACKGROUND, ignoreCase = true)) {
            entity.backgroundColor = null
            EntityManager.updateEntity(entity)
            success(player, "background-removed")
            return
        }

        if (Colors.parseHex(color) == null) return error(player, "invalid-color")

        entity.backgroundColor = color
        EntityManager.updateEntity(entity)
        success(player, "background-set", Placeholder.unparsed("color", color))
    }

    override fun complete(entity: BiteText, args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], listOf(NO_BACKGROUND)) + hint(args[0], "[#AARRGGBB]") else emptyList()
    }

    private companion object {
        const val NO_BACKGROUND = "none"
    }

}
