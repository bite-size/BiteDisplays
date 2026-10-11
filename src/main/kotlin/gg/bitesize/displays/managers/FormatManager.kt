package gg.bitesize.displays.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender

object FormatManager {

    private val miniMessage = MiniMessage.miniMessage()

    // Section-sign output with hex works on Spigot, Paper, Purpur, and the console
    private val legacySerializer = LegacyComponentSerializer.builder()
        .character(LegacyComponentSerializer.SECTION_CHAR)
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()

    private var prefix: Component = Component.empty()

    // &x&F&F&0&3&6&8 (Bungee hex), &#FF0368, and single codes like &a or &l, with & or §
    private val legacyRepeatedHex = Regex("[&§][xX]((?:[&§][0-9a-fA-F]){6})")
    private val legacyHex = Regex("[&§]#([0-9a-fA-F]{6})")
    private val legacyCode = Regex("[&§]([0-9a-fA-Fk-oK-OrR])")

    // A legacy color also clears bold, italic, and the rest, so colors start with <reset>
    private val legacyTags = mapOf(
        '0' to "<reset><black>", '1' to "<reset><dark_blue>", '2' to "<reset><dark_green>", '3' to "<reset><dark_aqua>",
        '4' to "<reset><dark_red>", '5' to "<reset><dark_purple>", '6' to "<reset><gold>", '7' to "<reset><gray>",
        '8' to "<reset><dark_gray>", '9' to "<reset><blue>", 'a' to "<reset><green>", 'b' to "<reset><aqua>",
        'c' to "<reset><red>", 'd' to "<reset><light_purple>", 'e' to "<reset><yellow>", 'f' to "<reset><white>",
        'k' to "<obfuscated>", 'l' to "<bold>", 'm' to "<strikethrough>", 'n' to "<underlined>", 'o' to "<italic>",
        'r' to "<reset>",
    )

    fun reload() {
        prefix = parseMiniMessage(ConfigManager.getPrefix())
    }

    /** Turns legacy codes (&a, &l, &#FF0368, &x&F&F...) into MiniMessage tags, so players can type either. */
    fun legacyToMiniMessage(text: String): String {
        if ('&' !in text && '§' !in text) return text

        return text
            .replace(legacyRepeatedHex) { "<reset><#${it.groupValues[1].filter { c -> c != '&' && c != '§' }}>" }
            .replace(legacyHex) { "<reset><#${it.groupValues[1]}>" }
            .replace(legacyCode) { legacyTags.getValue(it.groupValues[1].lowercase()[0]) }
    }

    /** The text without any formatting, to estimate how much space it takes up. */
    fun stripFormatting(text: String): String {
        return miniMessage.stripTags(legacyToMiniMessage(text))
    }

    fun parseMiniMessage(text: String, vararg placeholders: TagResolver): Component {
        return miniMessage.deserialize(legacyToMiniMessage(text), *placeholders)
    }

    fun toLegacy(component: Component): String {
        return legacySerializer.serialize(component)
    }

    /** MiniMessage straight to legacy text, for names, GUI items, and Spigot's String-only setters. */
    fun toLegacy(text: String, vararg placeholders: TagResolver): String {
        return toLegacy(parseMiniMessage(text, *placeholders))
    }

    fun getPrefix(): Component {
        return prefix
    }

    fun send(sender: CommandSender, text: String, usePrefix: Boolean = true,
             vararg placeholders: TagResolver) {

        var message = parseMiniMessage(text, *placeholders)

        if (usePrefix) {
            // Siblings, not a child, so the prefix's styling can't leak into the message
            message = Component.text().append(prefix, message).build()
        }

        sender.sendMessage(toLegacy(message))

    }

    fun error(sender: CommandSender, text: String, vararg placeholders: TagResolver) {
        send(sender, ConfigManager.getErrorPrefix() + text, true, *placeholders)
    }

}
