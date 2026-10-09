package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.commands.SubCommand
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.storage.EntityDatabase
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender

class SaveSubCommand : SubCommand("save", Permissions.ADMIN, playerOnly = false) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        EntityDatabase.save()
        success(sender, "saved", Placeholder.unparsed("count", EntityManager.getAll().size.toString()))
    }

}
