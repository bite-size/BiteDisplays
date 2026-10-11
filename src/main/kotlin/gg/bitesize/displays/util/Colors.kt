package gg.bitesize.displays.util

import org.bukkit.ChatColor
import org.bukkit.Color
import java.util.Locale

/**
 * Parses the glow and background colors players type into commands.
 *
 * Color names are vanilla's 16 chat colors, the only ones an NPC can glow in (its scoreboard team color).
 * Displays take any RGB, so a named color gives them the same RGB the client draws for that team color,
 * and a name glows the same on every type. Hex is displays only.
 */
object Colors {

    /** Vanilla's chat colors and the RGB the client draws for each. */
    private val NAMED = linkedMapOf(
        "black" to (ChatColor.BLACK to 0x000000),
        "dark_blue" to (ChatColor.DARK_BLUE to 0x0000AA),
        "dark_green" to (ChatColor.DARK_GREEN to 0x00AA00),
        "dark_aqua" to (ChatColor.DARK_AQUA to 0x00AAAA),
        "dark_red" to (ChatColor.DARK_RED to 0xAA0000),
        "dark_purple" to (ChatColor.DARK_PURPLE to 0xAA00AA),
        "gold" to (ChatColor.GOLD to 0xFFAA00),
        "gray" to (ChatColor.GRAY to 0xAAAAAA),
        "dark_gray" to (ChatColor.DARK_GRAY to 0x555555),
        "blue" to (ChatColor.BLUE to 0x5555FF),
        "green" to (ChatColor.GREEN to 0x55FF55),
        "aqua" to (ChatColor.AQUA to 0x55FFFF),
        "red" to (ChatColor.RED to 0xFF5555),
        "light_purple" to (ChatColor.LIGHT_PURPLE to 0xFF55FF),
        "yellow" to (ChatColor.YELLOW to 0xFFFF55),
        "white" to (ChatColor.WHITE to 0xFFFFFF),
    )

    /** Other names players reach for, as the chat color closest to them. */
    private val ALIASES = mapOf(
        "grey" to "gray",
        "dark_grey" to "dark_gray",
        "purple" to "light_purple",
        "pink" to "light_purple",
        "magenta" to "light_purple",
        "orange" to "gold",
        "lime" to "green",
    )

    /** The names for tab completion. */
    val GLOW_COLORS: List<String> = NAMED.keys.toList()

    /**
     * Parses a hex color string to a Bukkit Color.
     * Accepts #RRGGBB (6 chars) or #AARRGGBB (8 chars with alpha).
     */
    fun parseHex(hex: String): Color? {
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

    /** A display's glow color: a color name (in its chat color's RGB) or #RRGGBB / #AARRGGBB. */
    fun parse(input: String): Color? {
        return named(input)?.let { Color.fromRGB(it.second) } ?: parseHex(input)
    }

    /** The scoreboard team color for a color name, which is how NPCs glow. Hex can't be one. */
    fun teamColor(color: String): ChatColor? = named(color)?.first

    private fun named(input: String): Pair<ChatColor, Int>? {
        val name = input.lowercase(Locale.ROOT)
        return NAMED[ALIASES[name] ?: name]
    }

}
