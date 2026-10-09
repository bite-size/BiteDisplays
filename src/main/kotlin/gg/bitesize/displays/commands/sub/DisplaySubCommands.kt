package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.DisplaySubCommand
import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteBlock
import gg.bitesize.displays.models.BiteItem
import gg.bitesize.displays.models.BiteType
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

class ItemDisplaySubCommand : DisplaySubCommand<BiteItem>(
    BiteType.ITEM, Permissions.ITEM,
    extraActions = listOf("bouncing"),
) {

    override fun create(player: Player, held: ItemStack): BiteItem? {
        if (held.type.isAir) return null.also { error(player, "no-item") }

        return BiteItem(id = UUID.randomUUID(), location = player.location, itemStack = held.clone())
    }

    override fun execute(player: Player, display: BiteItem, action: String, label: String, args: List<String>) {
        if (action != "bouncing") return

        val bouncing = parseToggle(player, label, action, args) ?: return

        display.bouncing = bouncing
        EntityManager.updateEntity(display)
        success(player, if (bouncing) "bouncing-on" else "bouncing-off")
    }

    override fun tabComplete(sender: CommandSender, action: String, args: List<String>): List<String> {
        return if (action == "bouncing") toggleCompletions(args) else super.tabComplete(sender, action, args)
    }

}

class BlockDisplaySubCommand : DisplaySubCommand<BiteBlock>(BiteType.BLOCK, Permissions.BLOCK) {

    override fun create(player: Player, held: ItemStack): BiteBlock? {
        // Air counts as a block, so it's ruled out first
        if (held.type.isAir || !held.type.isBlock) return null.also { error(player, "no-block") }

        return BiteBlock(id = UUID.randomUUID(), location = player.location, itemStack = held.clone())
    }

}
