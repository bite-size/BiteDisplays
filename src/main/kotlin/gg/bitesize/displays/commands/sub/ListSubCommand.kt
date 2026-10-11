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

/**
 * `/bd list [type] [page]`: every entity, or one type, [PAGE_SIZE] to a page,
 * so a server with hundreds of entities builds a handful of lines per command.
 */
class ListSubCommand : SubCommand("list", Permissions.MANAGE, playerOnly = false) {

    private val typeKeys = BiteType.entries.map { it.key }

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        // The type and the page are both optional: /bd list, /bd list 2, /bd list text, /bd list text 2
        val typeArg = args.firstOrNull()?.takeIf { it.toIntOrNull() == null }
        val filter = typeArg?.lowercase(Locale.ROOT)?.let { key ->
            BiteType.entries.firstOrNull { it.key == key } ?: return sendUsage(sender, label)
        }
        val pageArg = args.getOrNull(if (typeArg == null) 0 else 1)
        if (args.size > (if (typeArg == null) 1 else 2)) return sendUsage(sender, label)

        val entities = EntityManager.getAll()
            .filter { filter == null || it.type == filter }
            .sortedWith(compareBy({ it.type }, { it.number }))
        if (entities.isEmpty()) return error(sender, "no-entities")

        val pages = (entities.size + PAGE_SIZE - 1) / PAGE_SIZE
        val page = if (pageArg == null) 1 else pageArg.toIntOrNull()?.takeIf { it in 1..pages }
            ?: return error(sender, "invalid-page", Placeholder.unparsed("pages", pages.toString()))

        FormatManager.send(sender, ConfigManager.getMessage("info.list-header"), true,
            Placeholder.unparsed("count", entities.size.toString()))

        for (entity in entities.subList((page - 1) * PAGE_SIZE, minOf(page * PAGE_SIZE, entities.size))) {
            val placeholders = arrayOf(entityPlaceholders(entity), locationPlaceholders(entity.location), details(entity))

            if (sender is Player) {
                val hover = FormatManager.toLegacy(ConfigManager.getMessage("info.list-select-hover"), entityPlaceholders(entity))
                val select = button("info.list-select", hover, "/$label select ${entity.displayId}")
                sendWithButtons(sender, "info.list-entry", mapOf("select" to select), placeholders)
            } else {
                // The console can't click, so the button is left out
                FormatManager.send(sender, ConfigManager.getMessage("info.list-entry"), false,
                    Placeholder.unparsed("select", ""), *placeholders)
            }
        }

        if (pages > 1) sendPageFooter(sender, label, filter, page, pages)
    }

    /** `info.list-page`, with buttons to the pages either side for players and a plain line for the console. */
    private fun sendPageFooter(sender: CommandSender, label: String, filter: BiteType?, page: Int, pages: Int) {
        val pagePlaceholders = arrayOf<TagResolver>(
            Placeholder.unparsed("page", page.toString()), Placeholder.unparsed("pages", pages.toString()))

        if (sender !is Player) {
            FormatManager.send(sender, ConfigManager.getMessage("info.list-page"), false,
                Placeholder.unparsed("previous", ""), Placeholder.unparsed("next", ""), *pagePlaceholders)
            return
        }

        val command = "/$label list" + (filter?.let { " ${it.key}" } ?: "")
        fun pageButton(key: String, target: Int): BaseComponent? {
            if (target !in 1..pages) return null
            val hover = FormatManager.toLegacy(ConfigManager.getMessage("info.list-page-hover"),
                Placeholder.unparsed("page", target.toString()))
            return button(key, hover, "$command $target")
        }

        val buttons = mapOf("previous" to pageButton("info.list-previous", page - 1), "next" to pageButton("info.list-next", page + 1))
        sendWithButtons(sender, "info.list-page", buttons, pagePlaceholders)
    }

    /** A clickable `[...]` from messages.yml that runs [command]. */
    @Suppress("DEPRECATION")
    private fun button(key: String, hover: String, command: String): BaseComponent {
        return TextComponent(*TextComponent.fromLegacyText(FormatManager.toLegacy(ConfigManager.getMessage(key)))).apply {
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
            hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(TextComponent.fromLegacyText(hover)))
        }
    }

    /**
     * Chat text is legacy text, which can't hold a click. So the line is rendered with a marker where each
     * button's placeholder goes, then rebuilt as Bungee chat components with the buttons in the markers' places.
     * A null button leaves its placeholder empty. Bungee chat is native on Spigot, and Paper still ships it (deprecated).
     */
    @Suppress("DEPRECATION")
    private fun sendWithButtons(player: Player, key: String, buttons: Map<String, BaseComponent?>, placeholders: Array<TagResolver>) {
        // A private-use character per button, so they never show up in real text
        val markers = buttons.keys.mapIndexed { i, name -> name to (MARKER_BASE + i).toString() }.toMap()
        val markerPlaceholders = buttons.map { (name, button) -> Placeholder.unparsed(name, if (button == null) "" else markers.getValue(name)) }
        val line = FormatManager.toLegacy(ConfigManager.getMessage(key), *markerPlaceholders.toTypedArray(), *placeholders)
        val byMarker = buttons.entries.mapNotNull { (name, button) -> button?.let { markers.getValue(name) to it } }

        // fromLegacyText gives flat parts that each carry their own formatting, so splitting one keeps its style
        var parts: List<BaseComponent> = TextComponent.fromLegacyText(line).toList()
        for ((marker, button) in byMarker) {
            parts = parts.flatMap { part ->
                val text = (part as? TextComponent)?.text
                if (text == null || part === button || marker !in text) return@flatMap listOf(part)

                text.split(marker).flatMapIndexed { i, piece ->
                    buildList {
                        if (i > 0) add(button)
                        if (piece.isNotEmpty()) add((part.duplicate() as TextComponent).apply { this.text = piece })
                    }
                }
            }
        }

        player.spigot().sendMessage(*parts.toTypedArray())
    }

    /** `<details>`: an NPC's formatted name, a display's material, or a text display's line count. */
    private fun details(entity: BiteEntity): TagResolver = when (entity) {
        is BiteNpc -> styledPlaceholder("details", entity.name ?: "")
        is BiteBlock -> Placeholder.unparsed("details", entity.itemStack.type.name.lowercase(Locale.ROOT))
        is BiteItem -> Placeholder.unparsed("details", entity.itemStack.type.name.lowercase(Locale.ROOT))
        is BiteText -> Placeholder.component("details", FormatManager.parseMiniMessage(
            ConfigManager.getMessage("info.list-lines"), Placeholder.unparsed("count", entity.lines.size.toString())))
        else -> Placeholder.unparsed("details", "")
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when {
            args.size == 1 -> matching(args[0], typeKeys) + hint(args[0], "[page]")
            args.size == 2 && args[0].toIntOrNull() == null -> hint(args[1], "[page]")
            else -> emptyList()
        }
    }

    private companion object {
        const val PAGE_SIZE = 10
        // The first private-use character, for button markers
        const val MARKER_BASE = '\uE000'
    }

}
