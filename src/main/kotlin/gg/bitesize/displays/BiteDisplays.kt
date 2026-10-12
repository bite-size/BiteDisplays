package gg.bitesize.displays

import gg.bitesize.displays.listeners.EntityInteractListener
import gg.bitesize.displays.listeners.GuiListener
import gg.bitesize.displays.listeners.PlayerListener
import gg.bitesize.displays.listeners.ProtectionListener
import gg.bitesize.displays.listeners.WorldListener
import gg.bitesize.displays.managers.AnimationManager
import gg.bitesize.displays.managers.CommandManager
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.platform.Platform
import gg.bitesize.displays.storage.EntityDatabase
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin

class BiteDisplays : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: BiteDisplays

        private const val BSTATS_PLUGIN_ID = 34592
    }

    private var metrics: Metrics? = null

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
        server.pluginManager.registerEvents(ProtectionListener, this)
        server.messenger.registerOutgoingPluginChannel(this, EntityInteractListener.PROXY_CHANNEL)

        AnimationManager.start()
        EntityDatabase.startAutosave()

        metrics = Metrics(this, BSTATS_PLUGIN_ID)

        logger.info("Loaded ${EntityManager.getAll().size} entities (${Platform.current.name} support).")
    }

    override fun onDisable() {
        metrics?.shutdown()

        CommandManager.shutdown()

        AnimationManager.stop()
        EntityDatabase.stopAutosave()

        EntityDatabase.saveNow()
        EntityManager.despawnAll()
    }

}
