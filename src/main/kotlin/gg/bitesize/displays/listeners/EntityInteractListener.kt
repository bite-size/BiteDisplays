package gg.bitesize.displays.listeners

import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteDisplay
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.ClickCommand
import gg.bitesize.displays.models.CommandRunner
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles players right-clicking NPCs and display and text hitboxes: plays the NPC's sound and runs the click command.
 */
object EntityInteractListener : Listener {

    private val lastClicks = ConcurrentHashMap<UUID, Long>()

    // @p alone, not a selector like @p[distance=..5]
    private val clickerSelector = Regex("@p(?![\\[\\w])")

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

        when (entity) {
            is BiteNpc -> {
                // String-based playSound for registry-backed sounds
                entity.sound?.let { player.playSound(entity.location, "minecraft:$it", 1.0f, 1.0f) }
                entity.command?.let { run(player, it) }
            }
            is BiteDisplay -> entity.command?.let { run(player, it) }
            is BiteText -> entity.command?.let { run(player, it) }
        }
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
            CommandRunner.PLAYER -> player.performCommand(command)
        }
    }
}
