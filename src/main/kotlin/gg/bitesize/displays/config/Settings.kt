package gg.bitesize.displays.config

import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.util.Colors
import org.bukkit.Color
import org.bukkit.configuration.ConfigurationSection
import java.util.EnumSet
import java.util.logging.Logger

/**
 * Immutable snapshot of config.yml, rebuilt on load and reload
 * so commands and animations never touch YAML on the hot path.
 */
class Settings private constructor(
    private val features: Set<BiteType>,
    val selectionRange: Double,
    /** Ticks a player waits between clicks on NPCs and displays. 0 lets every click through. */
    val clickCooldown: Long,
    val defaultTextLine: String,
    val defaultTextBackground: Color,
    val defaultTextBillboard: Boolean,
    val bounceInterval: Long,
    val bounceHeight: Float,
    val bounceSpeed: Float,
) {

    fun isEnabled(feature: BiteType): Boolean = feature in features

    companion object {

        // Vanilla's text display background
        private val DEFAULT_BACKGROUND = Color.fromARGB(64, 0, 0, 0)

        fun load(config: ConfigurationSection, logger: Logger): Settings {
            val features = EnumSet.noneOf(BiteType::class.java)
            BiteType.entries.filterTo(features) { config.getBoolean("features.${it.key}", true) }

            val backgroundHex = config.getString("text.default-background", "#40000000")!!
            val background = Colors.parseHex(backgroundHex) ?: run {
                logger.warning("Unknown color '$backgroundHex' in text.default-background. Use #RRGGBB or #AARRGGBB.")
                DEFAULT_BACKGROUND
            }

            return Settings(
                features = features,
                selectionRange = config.getDouble("selection.range", 5.0).coerceIn(1.0, 64.0),
                clickCooldown = config.getLong("clicks.cooldown", 0).coerceAtLeast(0),
                defaultTextLine = config.getString("text.default-line", "Text Display")!!,
                defaultTextBackground = background,
                defaultTextBillboard = config.getBoolean("text.default-billboard", true),
                bounceInterval = config.getLong("animations.bounce.interval", 4).coerceAtLeast(1),
                bounceHeight = config.getDouble("animations.bounce.height", 0.12).toFloat(),
                bounceSpeed = config.getDouble("animations.bounce.speed", 0.15).toFloat(),
            )
        }

    }

}
