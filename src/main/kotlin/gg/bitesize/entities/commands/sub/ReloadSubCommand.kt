package gg.bitesize.entities.commands.sub

import gg.bitesize.entities.commands.Permissions
import gg.bitesize.entities.commands.SubCommand
import gg.bitesize.entities.managers.AnimationManager
import gg.bitesize.entities.managers.ConfigManager
import gg.bitesize.entities.managers.EntityManager
import gg.bitesize.entities.models.BiteText
import org.bukkit.command.CommandSender

/** Reloads config.yml and messages.yml. Entities stay as they are; entities.json is only read on startup. */
class ReloadSubCommand : SubCommand("reload", Permissions.ADMIN, playerOnly = false) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        ConfigManager.reloadConfigs()
        AnimationManager.restart()

        // Text displays without their own background use the configured default
        EntityManager.getAll().filterIsInstance<BiteText>().forEach { EntityManager.updateEntity(it) }

        success(sender, "plugin-reloaded")
    }

}
