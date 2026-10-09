package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.BiteDisplays.Companion.INSTANCE
import gg.bitesize.displays.commands.EntitySubCommand
import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.gui.EquipmentGui
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.platform.Platform
import gg.bitesize.displays.util.Colors
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

class NpcSubCommand : EntitySubCommand<BiteNpc>(
    BiteType.NPC, Permissions.NPC,
    listOf("create", "skin", "cmd", "name", "glow", "desc", "equipment", "sound"),
) {

    // The sound registry doesn't change while the server runs. getKey() is deprecated on Spigot,
    // but its replacements don't exist on Paper, so it's the one accessor that works everywhere.
    @Suppress("DEPRECATION")
    private val soundKeys: Set<String> by lazy { Registry.SOUNDS.mapTo(sortedSetOf()) { it.key.key } }

    override fun execute(player: Player, action: String, label: String, args: List<String>) {
        if (action == "create") return create(player, label, args)

        val npc = selected(player, label) ?: return

        when (action) {
            "skin" -> skin(player, npc, label, args)
            "cmd" -> command(player, npc, args)
            "name" -> name(player, npc, args)
            "glow" -> glow(player, npc, args)
            "desc" -> description(player, npc, args)
            "equipment" -> EquipmentGui.open(player, npc)
            "sound" -> sound(player, npc, args)
        }
    }

    private fun create(player: Player, label: String, args: List<String>) {
        if (args.isEmpty()) return sendActionUsage(player, label, "create")

        spawnAndSelect(player, BiteNpc(id = UUID.randomUUID(), location = player.location, name = args.joinToString(" ")))
    }

    private fun skin(player: Player, npc: BiteNpc, label: String, args: List<String>) {
        val playerName = args.firstOrNull() ?: return sendActionUsage(player, label, "skin")
        val namePlaceholder = Placeholder.unparsed("player", playerName)

        success(player, "skin-fetching", namePlaceholder)

        val scheduler = INSTANCE.server.scheduler
        scheduler.runTaskAsynchronously(INSTANCE, Runnable {
            val skin = runCatching { Platform.current.fetchSkin(playerName) }
                .onFailure { INSTANCE.logger.warning("Couldn't fetch the skin of $playerName: ${it.message}") }
                .getOrNull()

            scheduler.runTask(INSTANCE, Runnable {
                if (skin == null) return@Runnable error(player, "skin-not-found", namePlaceholder)

                npc.skinValue = skin.value
                npc.skinSignature = skin.signature
                EntityManager.updateEntity(npc)
                success(player, "skin-applied", namePlaceholder)
            })
        })
    }

    private fun command(player: Player, npc: BiteNpc, args: List<String>) {
        if (args.isEmpty()) {
            npc.command = null
            success(player, "command-removed")
            return
        }

        val command = args.joinToString(" ")
        npc.command = command
        success(player, "command-set", Placeholder.unparsed("command", command))
    }

    private fun name(player: Player, npc: BiteNpc, args: List<String>) {
        if (args.isEmpty()) {
            npc.name = null
            EntityManager.updateEntity(npc)
            success(player, "name-removed")
            return
        }

        val name = args.joinToString(" ")
        npc.name = name
        EntityManager.updateEntity(npc)
        success(player, "name-set", Placeholder.parsed("name", name))
    }

    private fun glow(player: Player, npc: BiteNpc, args: List<String>) {
        if (args.isEmpty()) {
            npc.glowColor = null
            EntityManager.updateEntity(npc)
            success(player, "glow-removed")
            return
        }

        // NPCs glow through scoreboard teams, which only know the named chat colors
        val color = args[0].lowercase(Locale.ROOT)
        if (Colors.teamColor(color) == null) return error(player, "invalid-team-color")

        npc.glowColor = color
        EntityManager.updateEntity(npc)
        success(player, "glow-set", Placeholder.unparsed("color", color))
    }

    private fun description(player: Player, npc: BiteNpc, args: List<String>) {
        if (args.isEmpty()) {
            npc.description = null
            EntityManager.updateEntity(npc)
            success(player, "description-removed")
            return
        }

        val description = args.joinToString(" ")
        npc.description = description
        EntityManager.updateEntity(npc)
        success(player, "description-set", Placeholder.parsed("description", description))
    }

    private fun sound(player: Player, npc: BiteNpc, args: List<String>) {
        if (args.isEmpty()) {
            npc.sound = null
            success(player, "sound-removed")
            return
        }

        val sound = args[0].lowercase(Locale.ROOT).removePrefix("minecraft:")
        if (sound !in soundKeys) return error(player, "unknown-sound", Placeholder.unparsed("sound", sound))

        npc.sound = sound
        success(player, "sound-set", Placeholder.unparsed("sound", sound))
    }

    override fun tabComplete(sender: CommandSender, action: String, args: List<String>): List<String> {
        if (args.size != 1) return emptyList()
        val input = args[0]

        return when (action) {
            "create" -> hint(input, "<name>")
            "skin" -> matching(input, Bukkit.getOnlinePlayers().map { it.name })
            "cmd" -> hint(input, "[command]")
            "name" -> hint(input, "[name]")
            "glow" -> matching(input, Colors.GLOW_COLORS)
            "desc" -> hint(input, "[text]")
            "sound" -> soundKeys.filter { it.contains(input, ignoreCase = true) }
            else -> emptyList()
        }
    }

}
