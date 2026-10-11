package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.BiteDisplays.Companion.INSTANCE
import gg.bitesize.displays.commands.EditSubCommand
import gg.bitesize.displays.gui.EquipmentGui
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.platform.Platform
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Registry
import org.bukkit.entity.Player
import java.util.Locale

// Actions only NPCs have

/** `skin <player>`: fetched off the main thread, then applied on it. */
class SkinSubCommand : EditSubCommand<BiteNpc>("skin", BiteType.NPC) {

    override fun edit(player: Player, entity: BiteNpc, label: String, args: List<String>) {
        val playerName = args.firstOrNull() ?: return sendUsage(player, label)
        val namePlaceholder = Placeholder.unparsed("player", playerName)

        success(player, "skin-fetching", namePlaceholder)

        val scheduler = INSTANCE.server.scheduler
        scheduler.runTaskAsynchronously(INSTANCE, Runnable {
            val skin = runCatching { Platform.current.fetchSkin(playerName) }
                .onFailure { INSTANCE.logger.warning("Couldn't fetch the skin of $playerName: ${it.message}") }
                .getOrNull()

            scheduler.runTask(INSTANCE, Runnable {
                if (skin == null) return@Runnable error(player, "skin-not-found", namePlaceholder)

                entity.skinValue = skin.value
                entity.skinSignature = skin.signature
                EntityManager.updateEntity(entity)
                success(player, "skin-applied", namePlaceholder)
            })
        })
    }

    override fun complete(entity: BiteNpc, args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], Bukkit.getOnlinePlayers().map { it.name }) else emptyList()
    }

}

/** `desc [text]`: the line under the name. No text removes it. */
class DescriptionSubCommand : EditSubCommand<BiteNpc>("desc", BiteType.NPC) {

    override fun edit(player: Player, entity: BiteNpc, label: String, args: List<String>) {
        val description = args.joinToString(" ").takeIf { it.isNotBlank() }
        entity.description = description
        EntityManager.updateEntity(entity)

        if (description == null) success(player, "description-removed")
        else success(player, "description-set", styledPlaceholder("description", description))
    }

    override fun complete(entity: BiteNpc, args: List<String>): List<String> {
        return if (args.size == 1) hint(args[0], "[text]") else emptyList()
    }

}

/** `sound [sound]`: played to whoever clicks. No sound removes it. */
class SoundSubCommand : EditSubCommand<BiteNpc>("sound", BiteType.NPC) {

    // The sound registry doesn't change while the server runs. getKey() is deprecated on Spigot,
    // but its replacements don't exist on Paper, so it's the one accessor that works everywhere.
    @Suppress("DEPRECATION")
    private val soundKeys: Set<String> by lazy { Registry.SOUNDS.mapTo(sortedSetOf()) { it.key.key } }

    override fun edit(player: Player, entity: BiteNpc, label: String, args: List<String>) {
        // The sound only matters on click, so the entity itself doesn't change
        if (args.isEmpty()) {
            entity.sound = null
            success(player, "sound-removed")
            return
        }

        val sound = args[0].lowercase(Locale.ROOT).removePrefix("minecraft:")
        if (sound !in soundKeys) return error(player, "unknown-sound", Placeholder.unparsed("sound", sound))

        entity.sound = sound
        success(player, "sound-set", Placeholder.unparsed("sound", sound))
    }

    override fun complete(entity: BiteNpc, args: List<String>): List<String> {
        return if (args.size == 1) soundKeys.filter { it.contains(args[0], ignoreCase = true) } else emptyList()
    }

}

/** `equipment`: a menu for armor and held items. */
class EquipmentSubCommand : EditSubCommand<BiteNpc>("equipment", BiteType.NPC) {

    override fun edit(player: Player, entity: BiteNpc, label: String, args: List<String>) {
        EquipmentGui.open(player, entity)
    }

}
