package gg.bitesize.displays.platform

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.TextDisplay

/**
 * Paper and Purpur: text is rendered with Paper's own MiniMessage, so every tag works,
 * and skins go through Paper's ResolvableProfile. Loaded reflectively by [Platform.current].
 */
@Suppress("UnstableApiUsage")
class PaperPlatform : Platform {

    override val name = "Paper"

    private val miniMessage = MiniMessage.miniMessage()

    override fun setName(entity: Entity, text: String?) {
        entity.customName(text?.let { miniMessage.deserialize(it) })
    }

    override fun setText(display: TextDisplay, text: String) {
        display.text(miniMessage.deserialize(text))
    }

    override fun setDescription(mannequin: Mannequin, text: String?) {
        mannequin.description = text?.let { miniMessage.deserialize(it) }
    }

    override fun setSkin(mannequin: Mannequin, skin: Skin) {
        mannequin.profile = ResolvableProfile.resolvableProfile()
            .addProperty(ProfileProperty("textures", skin.value, skin.signature))
            .build()
    }

    override fun fetchSkin(playerName: String): Skin? {
        val profile = Bukkit.createProfile(playerName)
        profile.complete()

        return profile.properties.firstOrNull { it.name == "textures" }?.let { Skin(it.value, it.signature) }
    }

}
