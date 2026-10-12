package gg.bitesize.displays.listeners

import gg.bitesize.displays.BiteDisplays.Companion.INSTANCE
import gg.bitesize.displays.managers.CommandManager
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.ClickCommand
import gg.bitesize.displays.models.Clickable
import gg.bitesize.displays.models.CommandRunner
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles players right-clicking NPCs and display and text hitboxes: plays the NPC's sound and runs the click command.
 */
object EntityInteractListener : Listener {

    private val lastClicks = ConcurrentHashMap<UUID, Long>()

    // @p alone, not a selector like @p[distance=..5]
    private val clickerSelector = Regex("@p(?![\\[\\w])")
    private val whitespace = Regex("\\s+")

    /** Velocity reads it too, unless its bungee-plugin-message-channel is off. */
    const val PROXY_CHANNEL = "BungeeCord"

    /**
     * The only click handler. Since 26.3 a right-click fires PlayerInteractAtEntityEvent alone, and that subclass
     * shares this event's handler list, so it lands here. A second handler for it would run each click twice.
     */
    @EventHandler
    fun onInteract(event: PlayerInteractEntityEvent) {
        val entity = EntityManager.findByBukkitId(event.rightClicked.uniqueId) ?: return

        // Cancelled either way, so vanilla can't name, leash, or otherwise touch the entity
        event.isCancelled = true
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        if (!offCooldown(player)) return

        // String-based playSound for registry-backed sounds
        if (entity is BiteNpc) entity.sound?.let { player.playSound(entity.location, "minecraft:$it", 1.0f, 1.0f) }
        (entity as? Clickable)?.command?.let { run(player, it) }
    }

    fun forget(player: Player) {
        lastClicks.remove(player.uniqueId)
    }

    /** False if the player's last click was within `clicks.cooldown`. Clicks during the cooldown don't extend it. */
    private fun offCooldown(player: Player): Boolean {
        val cooldownMillis = ConfigManager.settings.clickCooldown * 50 // ticks to milliseconds
        if (cooldownMillis <= 0) return true

        val now = System.currentTimeMillis()
        val last = lastClicks[player.uniqueId]
        if (last != null && now - last < cooldownMillis) return false

        lastClicks[player.uniqueId] = now
        return true
    }

    private fun run(player: Player, click: ClickCommand) {
        // {player} is the original placeholder, kept so older commands still work
        val command = click.command.removePrefix("/")
            .replace(clickerSelector, player.name)
            .replace("{player}", player.name)

        when (click.runner) {
            CommandRunner.CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            CommandRunner.PLAYER -> if (!connectThroughProxy(player, command)) player.performCommand(command)
        }
    }

    /**
     * Sends `server <name>` to the proxy as a BungeeCord Connect message. performCommand runs on this server only, and
     * Velocity and BungeeCord only see commands the client sends, so their /server never ran from a click.
     * False (run it here instead) for anything else, or when a plugin on this server has its own /server.
     */
    private fun connectThroughProxy(player: Player, command: String): Boolean {
        val args = command.trim().split(whitespace)
        if (args.size != 2 || !args[0].equals("server", ignoreCase = true)) return false
        if (CommandManager.isRegistered("server")) return false

        val message = ByteArrayOutputStream()
        DataOutputStream(message).use {
            it.writeUTF("Connect")
            it.writeUTF(args[1])
        }
        player.sendPluginMessage(INSTANCE, PROXY_CHANNEL, message.toByteArray())
        return true
    }
}
