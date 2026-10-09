package gg.bitesize.displays.commands

import gg.bitesize.displays.commands.sub.BlockDisplaySubCommand
import gg.bitesize.displays.commands.sub.InfoSubCommand
import gg.bitesize.displays.commands.sub.ItemDisplaySubCommand
import gg.bitesize.displays.commands.sub.ListSubCommand
import gg.bitesize.displays.commands.sub.MoveHereSubCommand
import gg.bitesize.displays.commands.sub.NpcSubCommand
import gg.bitesize.displays.commands.sub.ReloadSubCommand
import gg.bitesize.displays.commands.sub.RemoveSubCommand
import gg.bitesize.displays.commands.sub.SaveSubCommand
import gg.bitesize.displays.commands.sub.SelectSubCommand
import gg.bitesize.displays.commands.sub.TextDisplaySubCommand
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.FormatManager
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

class DisplaysCommand : BaseCommand("bitedisplays", aliases = listOf("bd")) {

    // Declaration order is the help and tab-completion order
    private val subCommands: Map<String, SubCommand> = listOf(
        NpcSubCommand(),
        ItemDisplaySubCommand(),
        BlockDisplaySubCommand(),
        TextDisplaySubCommand(),
        SelectSubCommand(),
        MoveHereSubCommand(),
        RemoveSubCommand(),
        InfoSubCommand(),
        ListSubCommand(),
        SaveSubCommand(),
        ReloadSubCommand(),
    ).associateBy { it.name }

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
            sendHelp(sender, label)
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
            subCommand.feature != null && !ConfigManager.settings.isEnabled(subCommand.feature) ->
                error(sender, "feature-disabled")
            else ->
                subCommand.execute(sender, label, args.drop(1))
        }

        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        if (!sender.hasPermission(Permissions.USE)) return emptyList()

        if (args.size <= 1) {
            val input = args.firstOrNull() ?: ""
            val names = listOf("help") + subCommands.values.filter { it.isAvailableTo(sender) }.map { it.name }
            return names.filter { it.startsWith(input, ignoreCase = true) }
        }

        val subCommand = subCommands[args[0].lowercase(Locale.ROOT)]
            ?.takeIf { it.isAvailableTo(sender) }
            ?: return emptyList()

        return subCommand.tabComplete(sender, args.drop(1))
    }

    private fun sendHelp(sender: CommandSender, label: String) {
        val labelPlaceholder = Placeholder.unparsed("label", label)

        FormatManager.send(sender, ConfigManager.getMessage("help.header"), true, labelPlaceholder)
        subCommands.values
            .filter { it.isAvailableTo(sender) }
            .flatMap { it.helpKeys }
            .forEach { FormatManager.send(sender, ConfigManager.getMessage("help.$it"), false, labelPlaceholder) }
    }

    private fun error(sender: CommandSender, key: String) {
        FormatManager.error(sender, ConfigManager.getMessage("errors.$key"))
    }

}
