package gg.bitesize.plugin.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color

object FormatManager {

    // COLORS
    val PRIMARY = TextColor.fromHexString("#F0A6CA")
    val SECONDARY = TextColor.fromHexString("#99E1D9")
    val WHITE = TextColor.fromHexString("#F0E6EF")
    val BLACK = TextColor.fromHexString("#32292F")

    // MESSAGES
    val PREFIX = text("\uD83C\uDF70")
    const val NO_PERMISSION = "You don't have permission to run this command."
    const val PLAYER_ONLY = "Error: Only players can use this command."
    const val PLAYER_NOT_FOUND = "Error: Player not found."

    private val miniMessage = MiniMessage.miniMessage()

    // Named colors for tab completion
    val GLOW_COLORS = listOf(
        "red", "blue", "green", "yellow", "white", "black", "aqua",
        "purple", "orange", "lime", "pink", "gold", "gray",
        "dark_red", "dark_blue", "dark_green", "dark_aqua", "dark_purple", "dark_gray"
    )

    fun formatMessage(message: String, prefix: Boolean = true): TextComponent {
        if(!prefix) return text(message).color(WHITE)

        return text("[").color(SECONDARY).append(PREFIX.color(PRIMARY)).append(text("] ").color(SECONDARY))
            .append(text(message).color(WHITE))
    }

    fun formatMessageWithValue(message: String, value: String, prefix: Boolean = true): TextComponent {
        if(!prefix) return text(message).color(WHITE).append(text(" ")).append(text(value).color(PRIMARY))

        return text("[").color(SECONDARY).append(PREFIX.color(PRIMARY)).append(text("] ").color(SECONDARY))
            .append(text(message).color(WHITE)).append(text(" ")).append(text(value).color(PRIMARY))
    }

    /**
     * Parses a MiniMessage string (supports tags like <red>, <gradient>, <#FF0000>, etc.)
     */
    fun parseMiniMessage(input: String): Component {
        return miniMessage.deserialize(input)
    }

    /**
     * Parses a hex color string to a Bukkit Color.
     * Accepts #RRGGBB (6 chars) or #AARRGGBB (8 chars with alpha).
     */
    fun parseHexColor(hex: String): Color? {
        val clean = hex.removePrefix("#")
        return try {
            when (clean.length) {
                6 -> Color.fromRGB(clean.toInt(16))
                8 -> Color.fromARGB(clean.toLong(16).toInt())
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Parses a color by name or hex string to a Bukkit Color.
     * Supports named Minecraft colors and #RRGGBB / #AARRGGBB hex codes.
     */
    fun parseColor(input: String): Color? {
        return when (input.lowercase()) {
            "red" -> Color.RED
            "blue" -> Color.BLUE
            "green" -> Color.GREEN
            "yellow" -> Color.YELLOW
            "white" -> Color.WHITE
            "black" -> Color.fromRGB(0, 0, 0)
            "aqua" -> Color.AQUA
            "purple" -> Color.PURPLE
            "orange" -> Color.ORANGE
            "lime" -> Color.LIME
            "pink" -> Color.fromRGB(255, 105, 180)
            "gold" -> Color.fromRGB(255, 215, 0)
            "gray", "grey" -> Color.GRAY
            "dark_red" -> Color.fromRGB(170, 0, 0)
            "dark_blue" -> Color.fromRGB(0, 0, 170)
            "dark_green" -> Color.fromRGB(0, 170, 0)
            "dark_aqua" -> Color.fromRGB(0, 170, 170)
            "dark_purple" -> Color.fromRGB(170, 0, 170)
            "dark_gray", "dark_grey" -> Color.fromRGB(85, 85, 85)
            else -> parseHexColor(input)
        }
    }

    /**
     * Converts a color name string to a NamedTextColor for scoreboard teams.
     * Only supports the 16 standard Minecraft chat colors (no hex).
     */
    fun namedTextColorFromString(color: String): NamedTextColor? {
        return when (color.lowercase()) {
            "red" -> NamedTextColor.RED
            "blue" -> NamedTextColor.BLUE
            "green" -> NamedTextColor.GREEN
            "yellow" -> NamedTextColor.YELLOW
            "white" -> NamedTextColor.WHITE
            "black" -> NamedTextColor.BLACK
            "aqua" -> NamedTextColor.AQUA
            "purple", "light_purple" -> NamedTextColor.LIGHT_PURPLE
            "gold", "orange" -> NamedTextColor.GOLD
            "gray", "grey" -> NamedTextColor.GRAY
            "dark_red" -> NamedTextColor.DARK_RED
            "dark_blue" -> NamedTextColor.DARK_BLUE
            "dark_green" -> NamedTextColor.DARK_GREEN
            "dark_aqua" -> NamedTextColor.DARK_AQUA
            "dark_purple" -> NamedTextColor.DARK_PURPLE
            "dark_gray", "dark_grey" -> NamedTextColor.DARK_GRAY
            else -> null
        }
    }
}