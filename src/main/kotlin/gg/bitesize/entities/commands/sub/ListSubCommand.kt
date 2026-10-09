package gg.bitesize.entities.commands.sub

import gg.bitesize.entities.commands.Permissions
import gg.bitesize.entities.commands.SubCommand
import gg.bitesize.entities.managers.ConfigManager
import gg.bitesize.entities.managers.EntityManager
import gg.bitesize.entities.managers.FormatManager
import gg.bitesize.entities.models.BiteBlock
import gg.bitesize.entities.models.BiteEntity
import gg.bitesize.entities.models.BiteItem
import gg.bitesize.entities.models.BiteNpc
import gg.bitesize.entities.models.BiteText
import gg.bitesize.entities.models.BiteType
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import java.util.Locale

class ListSubCommand : SubCommand("list", Permissions.MANAGE, playerOnly = false) {

    private val typeKeys = BiteType.entries.map { it.key }

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val filter = args.firstOrNull()?.lowercase(Locale.ROOT)?.let { key ->
            BiteType.entries.firstOrNull { it.key == key } ?: return sendUsage(sender, label)
        }

        val entities = EntityManager.getAll().filter { filter == null || it.type == filter }
        if (entities.isEmpty()) return error(sender, "no-entities")

        FormatManager.send(sender, ConfigManager.getMessage("info.list-header"), true,
            Placeholder.unparsed("count", entities.size.toString()))

        for (entity in entities) {
            FormatManager.send(sender, ConfigManager.getMessage("info.list-entry"), false,
                entityPlaceholders(entity),
                locationPlaceholders(entity.location),
                details(entity))
        }
    }

    /** `<details>`: an NPC's name, a display's material, or a text display's line count. */
    private fun details(entity: BiteEntity): TagResolver = when (entity) {
        is BiteNpc -> Placeholder.unparsed("details", entity.name ?: "")
        is BiteBlock -> Placeholder.unparsed("details", entity.itemStack.type.name.lowercase(Locale.ROOT))
        is BiteItem -> Placeholder.unparsed("details", entity.itemStack.type.name.lowercase(Locale.ROOT))
        is BiteText -> Placeholder.component("details", FormatManager.parseMiniMessage(
            ConfigManager.getMessage("info.list-lines"), Placeholder.unparsed("count", entity.lines.size.toString())))
        else -> Placeholder.unparsed("details", "")
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], typeKeys) else emptyList()
    }

}
