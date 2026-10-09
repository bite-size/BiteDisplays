package gg.bitesize.entities.commands.sub

import gg.bitesize.entities.commands.Permissions
import gg.bitesize.entities.commands.SubCommand
import gg.bitesize.entities.managers.EntityManager
import gg.bitesize.entities.storage.EntityDatabase
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender

class SaveSubCommand : SubCommand("save", Permissions.ADMIN, playerOnly = false) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        EntityDatabase.save()
        success(sender, "saved", Placeholder.unparsed("count", EntityManager.getAll().size.toString()))
    }

}
