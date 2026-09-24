@file:Suppress("UnstableApiUsage")
package gg.bitesize.plugin

import gg.bitesize.plugin.commands.EntityCommand
import gg.bitesize.plugin.listeners.EntityInteractListener
import gg.bitesize.plugin.listeners.GuiListener
import gg.bitesize.plugin.listeners.WorldListener
import gg.bitesize.plugin.managers.AnimationManager
import gg.bitesize.plugin.managers.EntityManager
import gg.bitesize.plugin.storage.EntityDatabase
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class Entities : JavaPlugin() {

    override fun onEnable() {
        // Storage
        EntityDatabase.initialize(dataFolder)
        EntityDatabase.load()

        // Commands — registered via lifecycle API (bypasses Brigadier arg parsing for @ support)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register("biteentities", "Manage BiteEntities NPCs and displays", listOf("be"), EntityCommand(this))
        }

        // Listeners
        server.pluginManager.registerEvents(EntityInteractListener(), this)
        server.pluginManager.registerEvents(WorldListener(), this)
        server.pluginManager.registerEvents(GuiListener(), this)

        // Animations
        AnimationManager.start(this)

        logger.info("BiteEntities enabled — ${EntityManager.getAll().size} entities loaded.")
    }

    override fun onDisable() {
        AnimationManager.stop()
        EntityDatabase.save()
        EntityManager.despawnAll()
        logger.info("BiteEntities disabled — entities saved.")
    }
}