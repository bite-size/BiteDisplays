package gg.bitesize.displays.listeners

import gg.bitesize.displays.BiteDisplays
import gg.bitesize.displays.managers.EntityManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent

/** A plugin's NPC overrides end when it disables, so a stopped plugin never leaves another look behind. */
object PluginListener : Listener {

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        // Our own disable removes every entity anyway
        if (event.plugin === BiteDisplays.INSTANCE) return
        EntityManager.clearNpcOverridesOf(event.plugin.name)
    }

}
