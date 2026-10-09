package gg.bitesize.entities

import gg.bitesize.entities.listeners.EntityInteractListener
import gg.bitesize.entities.listeners.GuiListener
import gg.bitesize.entities.listeners.PlayerListener
import gg.bitesize.entities.listeners.WorldListener
import gg.bitesize.entities.managers.AnimationManager
import gg.bitesize.entities.managers.CommandManager
import gg.bitesize.entities.managers.ConfigManager
import gg.bitesize.entities.managers.EntityManager
import gg.bitesize.entities.platform.Platform
import gg.bitesize.entities.storage.EntityDatabase
import org.bukkit.plugin.java.JavaPlugin

class BiteEntities : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: BiteEntities
    }

    override fun onLoad() {
        INSTANCE = this
    }

    override fun onEnable() {
        ConfigManager.loadConfigs()

        EntityDatabase.initialize()
        EntityDatabase.load()

        CommandManager.registerAll()
        CommandManager.syncCommands()

        server.pluginManager.registerEvents(EntityInteractListener, this)
        server.pluginManager.registerEvents(WorldListener, this)
        server.pluginManager.registerEvents(GuiListener, this)
        server.pluginManager.registerEvents(PlayerListener, this)

        AnimationManager.start()

        logger.info("Loaded ${EntityManager.getAll().size} entities (${Platform.current.name} support).")
    }

    override fun onDisable() {
        CommandManager.shutdown()

        AnimationManager.stop()

        EntityDatabase.save()
        EntityManager.despawnAll()
    }

}
