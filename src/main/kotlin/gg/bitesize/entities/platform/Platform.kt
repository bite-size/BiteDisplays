package gg.bitesize.entities.platform

import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.TextDisplay

/** A skin's signed `textures` property, as stored in entities.json. */
data class Skin(val value: String, val signature: String?)

/**
 * The calls that differ between Spigot and Paper. Paper split from Spigot before mannequins
 * existed, so their Mannequin APIs don't overlap. Text arrives as MiniMessage, so Paper can
 * render it natively while Spigot gets legacy text.
 *
 * Signatures must stay free of Adventure types: the Paper side uses Paper's own Adventure,
 * while ours is relocated.
 */
interface Platform {

    val name: String

    /** Sets or clears (null) an entity's visible name. */
    fun setName(entity: Entity, text: String?)

    fun setText(display: TextDisplay, text: String)

    /** Sets the line below a mannequin's name, or restores the default label (null). */
    fun setDescription(mannequin: Mannequin, text: String?)

    fun setSkin(mannequin: Mannequin, skin: Skin)

    /** Looks up a player's current skin. Blocks on a web request, so call it off the main thread. */
    fun fetchSkin(playerName: String): Skin?

    companion object {

        private const val PAPER_PROFILE = "io.papermc.paper.datacomponent.item.ResolvableProfile"
        private const val PAPER_PLATFORM = "gg.bitesize.entities.platform.PaperPlatform"

        val current: Platform by lazy {
            val isPaper = runCatching { Class.forName(PAPER_PROFILE) }.isSuccess
            if (isPaper) {
                Class.forName(PAPER_PLATFORM).getDeclaredConstructor().newInstance() as Platform
            } else {
                SpigotPlatform
            }
        }

    }

}
