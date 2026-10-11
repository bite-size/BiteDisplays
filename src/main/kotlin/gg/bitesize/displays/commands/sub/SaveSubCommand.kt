package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.commands.SubCommand
import gg.bitesize.displays.BiteDisplays.Companion.INSTANCE
import gg.bitesize.displays.storage.EntityDatabase
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class SaveSubCommand : SubCommand("save", Permissions.ADMIN, playerOnly = false) {

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        // Written off the main thread, then answered back on it
        EntityDatabase.saveAsync().whenComplete { count, error ->
            Bukkit.getScheduler().runTask(INSTANCE, Runnable {
                if (error != null) error(sender, "save-failed")
                else success(sender, "saved", Placeholder.unparsed("count", count.toString()))
            })
        }
    }

}
