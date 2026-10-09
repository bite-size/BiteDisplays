package gg.bitesize.displays.util

import org.bukkit.ChatColor
import org.bukkit.Color
import java.util.Locale

/** Parses the glow and background colors players type into commands. */
object Colors {

    // Named colors for tab completion
    val GLOW_COLORS = listOf(
        "red", "blue", "green", "yellow", "white", "black", "aqua",
        "purple", "orange", "lime", "pink", "gold", "gray",
        "dark_red", "dark_blue", "dark_green", "dark_aqua", "dark_purple", "dark_gray"
    )

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

    /**
     * Parses a color by name or hex string to a Bukkit Color.
     * Supports named Minecraft colors and #RRGGBB / #AARRGGBB hex codes.
     */
    fun parse(input: String): Color? {
        return when (input.lowercase(Locale.ROOT)) {
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
            else -> parseHex(input)
        }
    }

    /**
     * Converts a color name to a ChatColor for scoreboard teams, which is how NPCs glow.
     * Only supports the 16 standard Minecraft chat colors (no hex).
     */
    fun teamColor(color: String): ChatColor? {
        return when (color.lowercase(Locale.ROOT)) {
            "red" -> ChatColor.RED
            "blue" -> ChatColor.BLUE
            "green", "lime" -> ChatColor.GREEN
            "yellow" -> ChatColor.YELLOW
            "white" -> ChatColor.WHITE
            "black" -> ChatColor.BLACK
            "aqua" -> ChatColor.AQUA
            "purple", "light_purple", "pink" -> ChatColor.LIGHT_PURPLE
            "gold", "orange" -> ChatColor.GOLD
            "gray", "grey" -> ChatColor.GRAY
            "dark_red" -> ChatColor.DARK_RED
            "dark_blue" -> ChatColor.DARK_BLUE
            "dark_green" -> ChatColor.DARK_GREEN
            "dark_aqua" -> ChatColor.DARK_AQUA
            "dark_purple" -> ChatColor.DARK_PURPLE
            "dark_gray", "dark_grey" -> ChatColor.DARK_GRAY
            else -> null
        }
    }

}
