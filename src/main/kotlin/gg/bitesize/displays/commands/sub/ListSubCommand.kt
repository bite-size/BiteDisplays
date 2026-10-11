package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.commands.SubCommand
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.managers.FormatManager
import gg.bitesize.displays.models.BiteBlock
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteItem
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.BiteType
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

class ListSubCommand : SubCommand("list", Permissions.MANAGE, playerOnly = false) {

    private val typeKeys = BiteType.entries.map { it.key }

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val filter = args.firstOrNull()?.lowercase(Locale.ROOT)?.let { key ->
            BiteType.entries.firstOrNull { it.key == key } ?: return sendUsage(sender, label)
        }

        val entities = EntityManager.getAll()
            .filter { filter == null || it.type == filter }
            .sortedWith(compareBy({ it.type }, { it.number }))
        if (entities.isEmpty()) return error(sender, "no-entities")

        FormatManager.send(sender, ConfigManager.getMessage("info.list-header"), true,
            Placeholder.unparsed("count", entities.size.toString()))

        for (entity in entities) {
            val placeholders = arrayOf(entityPlaceholders(entity), locationPlaceholders(entity.location), details(entity))

            if (sender is Player) {
                sendWithButton(sender, label, entity, placeholders)
            } else {
                // The console can't click, so it gets a plain bullet
                FormatManager.send(sender, ConfigManager.getMessage("info.list-entry"), false,
                    Placeholder.unparsed("select", "-"), *placeholders)
            }
        }
    }

    /**
     * Chat text is legacy text, which can't hold a click. So the line is rendered with a marker where
     * `<select>` goes, then rebuilt as Bungee chat components with the button in the marker's place.
     * Bungee chat is native on Spigot, and Paper still ships it (deprecated).
     */
    @Suppress("DEPRECATION")
    private fun sendWithButton(player: Player, label: String, entity: BiteEntity, placeholders: Array<TagResolver>) {
        val line = FormatManager.toLegacy(ConfigManager.getMessage("info.list-entry"),
            Placeholder.unparsed("select", BUTTON_MARKER), *placeholders)

        val hover = FormatManager.toLegacy(ConfigManager.getMessage("info.list-select-hover"), entityPlaceholders(entity))
        val button = TextComponent(*TextComponent.fromLegacyText(FormatManager.toLegacy(ConfigManager.getMessage("info.list-select")))).apply {
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$label select ${entity.displayId}")
            hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(TextComponent.fromLegacyText(hover)))
        }

        // fromLegacyText gives flat parts that each carry their own formatting, so splitting one keeps its style
        val parts = TextComponent.fromLegacyText(line).flatMap { part ->
            val text = (part as? TextComponent)?.text
            if (text == null || BUTTON_MARKER !in text) return@flatMap listOf(part)

            text.split(BUTTON_MARKER).flatMapIndexed { i, piece ->
                buildList<BaseComponent> {
                    if (i > 0) add(button)
                    if (piece.isNotEmpty()) add((part.duplicate() as TextComponent).apply { this.text = piece })
                }
            }
        }

        player.spigot().sendMessage(*parts.toTypedArray())
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

    private companion object {
        // A private-use character, so it never shows up in real text
        const val BUTTON_MARKER = ""
    }

}
