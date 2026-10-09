package gg.bitesize.displays.listeners

import gg.bitesize.displays.managers.EntityManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        EntityManager.clearSelection(event.player.uniqueId)
    }

}
