package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.commands.SubCommand
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.managers.FormatManager
import gg.bitesize.displays.models.BiteBlock
import gg.bitesize.displays.models.BiteDisplay
import gg.bitesize.displays.models.BiteItem
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.ClickCommand
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

/** Selects the entity the player looks at, or one by ID (what the [SELECT] buttons in /bd list run). */
class SelectSubCommand : SubCommand("select", Permissions.MANAGE) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val player = sender as Player
        val id = args.firstOrNull()

        val entity = if (id != null) {
            EntityManager.findByDisplayId(id) ?: return error(player, "unknown-id", Placeholder.unparsed("id", id))
        } else {
            val range = settings.selectionRange
            EntityManager.findInSight(player.eyeLocation, range)
                ?: return error(player, "nothing-in-sight", Placeholder.unparsed("range", number(range)))
        }

        EntityManager.select(player.uniqueId, entity.id)
        success(player, "selected", entityPlaceholders(entity))
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], entityIds()) else emptyList()
    }

}

class MoveHereSubCommand : SubCommand("movehere", Permissions.MANAGE) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val player = sender as Player
        val entity = selection(player, label) ?: return

        EntityManager.teleport(entity.id, EntityManager.placement(entity.type, player.location))
        success(player, "moved", entityPlaceholders(entity))
    }

}

class RemoveSubCommand : SubCommand("remove", Permissions.MANAGE) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val player = sender as Player
        val entity = selection(player, label) ?: return

        EntityManager.despawn(entity.id)
        success(player, "removed", entityPlaceholders(entity))
    }

}

class InfoSubCommand : SubCommand("info", Permissions.MANAGE) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val player = sender as Player
        val entity = selection(player, label) ?: return

        val none = info("none")
        line(player, "header", entityPlaceholders(entity))
        line(player, "location", locationPlaceholders(entity.location))

        when (entity) {
            is BiteNpc -> {
                field(player, "name", entity.name ?: none)
                field(player, "description", entity.description ?: none)
                field(player, "size", number(entity.scale))
                field(player, "skin", if (entity.skinValue != null) info("custom") else info("default"))
                command(player, entity.command)
                field(player, "sound", entity.sound ?: none)
                field(player, "glow", entity.glowColor ?: none)
            }
            is BiteDisplay -> {
                val material = when (entity) {
                    is BiteBlock -> entity.itemStack.type
                    is BiteItem -> entity.itemStack.type
                    else -> null
                }
                field(player, "material", material?.name?.lowercase(Locale.ROOT) ?: none)
                field(player, "size", number(entity.scale.x))
                line(player, "rotation",
                    Placeholder.unparsed("yaw", number(entity.yaw)), Placeholder.unparsed("pitch", number(entity.pitch)))
                field(player, "name", entity.nameText ?: none)
                command(player, entity.command)
                field(player, "glow", entity.glowColor ?: none)
                if (entity is BiteItem) field(player, "bouncing", yesNo(entity.bouncing))
            }
            is BiteText -> {
                field(player, "size", number(entity.scale.x))
                field(player, "background", entity.backgroundColor ?: info("default"))
                field(player, "glow", entity.glowColor ?: none)
                field(player, "billboard", yesNo(entity.billboard))
                field(player, "lines", entity.lines.size.toString())
                entity.lines.forEachIndexed { i, text ->
                    line(player, "line", Placeholder.unparsed("line", (i + 1).toString()), Placeholder.unparsed("value", text))
                }
            }
        }
    }

    private fun info(key: String): String = ConfigManager.getMessage("info.$key")

    private fun yesNo(value: Boolean): String = info(if (value) "enabled" else "disabled")

    /** The click command, then who runs it. */
    private fun command(player: Player, command: ClickCommand?) {
        field(player, "command", command?.command ?: info("none"))
        if (command != null) line(player, "command-runner", runnerPlaceholder(command.runner))
    }

    /** Sends `info.<key>` with the raw [value], so admins see the MiniMessage they typed. */
    private fun field(player: Player, key: String, value: String) {
        line(player, key, Placeholder.unparsed("value", value))
    }

    private fun line(player: Player, key: String, vararg placeholders: TagResolver) {
        FormatManager.send(player, info(key), key == "header", *placeholders)
    }

}
