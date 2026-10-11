package gg.bitesize.displays.commands

import gg.bitesize.displays.commands.sub.AddLineSubCommand
import gg.bitesize.displays.commands.sub.BackgroundSubCommand
import gg.bitesize.displays.commands.sub.CommandSubCommand
import gg.bitesize.displays.commands.sub.CreateSubCommand
import gg.bitesize.displays.commands.sub.DeleteLineSubCommand
import gg.bitesize.displays.commands.sub.DescriptionSubCommand
import gg.bitesize.displays.commands.sub.EditLineSubCommand
import gg.bitesize.displays.commands.sub.EquipmentSubCommand
import gg.bitesize.displays.commands.sub.GlowSubCommand
import gg.bitesize.displays.commands.sub.InfoSubCommand
import gg.bitesize.displays.commands.sub.ListSubCommand
import gg.bitesize.displays.commands.sub.MoveHereSubCommand
import gg.bitesize.displays.commands.sub.NameSubCommand
import gg.bitesize.displays.commands.sub.ReloadSubCommand
import gg.bitesize.displays.commands.sub.RemoveSubCommand
import gg.bitesize.displays.commands.sub.RotateSubCommand
import gg.bitesize.displays.commands.sub.SaveSubCommand
import gg.bitesize.displays.commands.sub.SelectSubCommand
import gg.bitesize.displays.commands.sub.SetBlockSubCommand
import gg.bitesize.displays.commands.sub.SetItemSubCommand
import gg.bitesize.displays.commands.sub.SetLocationSubCommand
import gg.bitesize.displays.commands.sub.SizeSubCommand
import gg.bitesize.displays.commands.sub.SkinSubCommand
import gg.bitesize.displays.commands.sub.SoundSubCommand
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.FormatManager
import gg.bitesize.displays.models.BiteItem
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.BiteType
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

/**
 * `/bitedisplays`, alias `/bd`. Editing actions work on the player's selection, whatever its type,
 * so `/bd help` lists the rest and `/bd help <type>` lists one type's actions.
 */
class DisplaysCommand : BaseCommand("bitedisplays", aliases = listOf("bd")) {

    // Declaration order is the help and tab-completion order
    private val subCommands: Map<String, SubCommand> = listOf(
        CreateSubCommand(),
        NameSubCommand(),
        SetBlockSubCommand(),
        SetItemSubCommand(),
        DescriptionSubCommand(),
        AddLineSubCommand(),
        EditLineSubCommand(),
        DeleteLineSubCommand(),
        SkinSubCommand(),
        EquipmentSubCommand(),
        SizeSubCommand(),
        RotateSubCommand(),
        GlowSubCommand(),
        BackgroundSubCommand(),
        ToggleSubCommand<BiteText>("shadow", BiteType.TEXT) { text, on -> text.shadow = on },
        ToggleSubCommand<BiteText>("billboard", BiteType.TEXT) { text, on -> text.billboard = on },
        ToggleSubCommand<BiteItem>("bouncing", BiteType.ITEM) { item, on -> item.bouncing = on },
        CommandSubCommand(),
        SoundSubCommand(),
        SelectSubCommand(),
        MoveHereSubCommand(),
        SetLocationSubCommand(),
        RemoveSubCommand(),
        InfoSubCommand(),
        ListSubCommand(),
        SaveSubCommand(),
        ReloadSubCommand(),
    ).associateBy { it.name }

    private val editCommands get() = subCommands.values.filterIsInstance<EditSubCommand<*>>()

    init {
        // Hides the command from players who can't use it
        permission = Permissions.USE
    }

    override fun execute(sender: CommandSender, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission(Permissions.USE)) {
            error(sender, "no-permission")
            return true
        }

        val name = args.firstOrNull()?.lowercase(Locale.ROOT)
        if (name == null || name == "help") {
            sendHelp(sender, label, args.getOrNull(1))
            return true
        }

        val subCommand = subCommands[name]
        when {
            subCommand == null ->
                error(sender, "unknown-command")
            subCommand.playerOnly && sender !is Player ->
                error(sender, "players-only")
            !sender.hasPermission(subCommand.permission) ->
                error(sender, "no-permission")
            else ->
                subCommand.execute(sender, label, args.drop(1))
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        if (!sender.hasPermission(Permissions.USE)) return emptyList()

        if (args.size <= 1) {
            val input = args.firstOrNull() ?: ""
            // Editing actions that don't work on the selection are left out
            val names = listOf("help") + subCommands.values
                .filter { if (it is EditSubCommand<*>) it.isSuggestedTo(sender) else it.isAvailableTo(sender) }
                .map { it.name }
            return names.filter { it.startsWith(input, ignoreCase = true) }
        }

        if (args[0].equals("help", ignoreCase = true)) {
            if (args.size != 2) return emptyList()
            return helpTypes(sender).map { it.key }.filter { it.startsWith(args[1], ignoreCase = true) }
        }

        val subCommand = subCommands[args[0].lowercase(Locale.ROOT)]
            ?.takeIf { it.isAvailableTo(sender) }
            ?: return emptyList()

        return subCommand.tabComplete(sender, args.drop(1))
    }

    /** The types whose actions [sender] can list with `/bd help <type>`. */
    private fun helpTypes(sender: CommandSender): List<BiteType> {
        return BiteType.entries.filter { SubCommand.canUse(sender, it) }
    }

    /** `/bd help`: everything but the editing actions, which `/bd help <type>` lists per type. */
    private fun sendHelp(sender: CommandSender, label: String, typeKey: String?) {
        val labelPlaceholder = Placeholder.unparsed("label", label)
        if (typeKey != null) return sendTypeHelp(sender, labelPlaceholder, typeKey)

        send(sender, "header", labelPlaceholder)
        subCommands.values
            .filter { it !is EditSubCommand<*> && it.isAvailableTo(sender) }
            .forEach { send(sender, it.name, labelPlaceholder) }
        if (editCommands.any { it.isAvailableTo(sender) }) send(sender, "types", labelPlaceholder)
    }

    /** `/bd help <type>`: how to create one, then every action that works on it. */
    private fun sendTypeHelp(sender: CommandSender, labelPlaceholder: TagResolver, typeKey: String) {
        val type = BiteType.entries.firstOrNull { it.key == typeKey.lowercase(Locale.ROOT) }
            ?: return FormatManager.error(sender, ConfigManager.getMessage("errors.usage-help"), labelPlaceholder)

        if (!sender.hasPermission(Permissions.forType(type))) return error(sender, "no-permission")
        if (!ConfigManager.settings.isEnabled(type)) return error(sender, "feature-disabled")

        send(sender, "${type.key}-header", labelPlaceholder)
        send(sender, "create-${type.key}", labelPlaceholder)
        editCommands.filter { type in it.types }.forEach { send(sender, it.name, labelPlaceholder) }
        send(sender, "optional-note", labelPlaceholder)
    }

    /** Sends `help.<key>`. Headers get the prefix. */
    private fun send(sender: CommandSender, key: String, labelPlaceholder: TagResolver) {
        val prefixed = key == "header" || key.endsWith("-header")
        FormatManager.send(sender, ConfigManager.getMessage("help.$key"), prefixed, labelPlaceholder)
    }

    private fun error(sender: CommandSender, key: String) {
        FormatManager.error(sender, ConfigManager.getMessage("errors.$key"))
    }

}
