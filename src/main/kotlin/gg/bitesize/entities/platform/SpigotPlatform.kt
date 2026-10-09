package gg.bitesize.entities.platform

import gg.bitesize.entities.managers.FormatManager
import org.bukkit.Bukkit
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.TextDisplay
import org.bukkit.profile.PlayerProfile

/**
 * Spigot only takes legacy text, so MiniMessage is rendered to section-sign text with hex colors.
 * Fonts, translations, and object tags like <head> don't survive that.
 */
object SpigotPlatform : Platform {

    override val name = "Spigot"

    // A profile with only textures, like vanilla's {profile:{properties:[...]}}
    private const val NIL_UUID = "00000000-0000-0000-0000-000000000000"

    override fun setName(entity: Entity, text: String?) {
        entity.customName = text?.let { FormatManager.toLegacy(it) }
    }

    override fun setText(display: TextDisplay, text: String) {
        display.text = FormatManager.toLegacy(text)
    }

    override fun setDescription(mannequin: Mannequin, text: String?) {
        mannequin.description = text?.let { FormatManager.toLegacy(it) }
    }

    override fun setSkin(mannequin: Mannequin, skin: Skin) {
        // The API can't set a raw property, but a deserialized profile keeps the signature
        val property = buildMap {
            put("name", "textures")
            put("value", skin.value)
            skin.signature?.let { put("signature", it) }
        }
        val profile = ConfigurationSerialization.deserializeObject(mapOf(
            ConfigurationSerialization.SERIALIZED_TYPE_KEY to "PlayerProfile",
            "uniqueId" to NIL_UUID,
            "properties" to listOf(property),
        )) as PlayerProfile

        mannequin.playerProfile = profile
    }

    override fun fetchSkin(playerName: String): Skin? {
        val profile = Bukkit.createPlayerProfile(playerName).update().get()

        // Serializing is the only way to read the signed property back out
        val properties = profile.serialize()["properties"] as? List<*> ?: return null
        val textures = properties.filterIsInstance<Map<*, *>>().firstOrNull { it["name"] == "textures" } ?: return null
        val value = textures["value"] as? String ?: return null

        return Skin(value, textures["signature"] as? String)
    }

}
