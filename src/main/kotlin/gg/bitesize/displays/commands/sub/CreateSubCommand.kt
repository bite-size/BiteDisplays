package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.Permissions
import gg.bitesize.displays.commands.SubCommand
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteBlock
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteItem
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.BiteType
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

/**
 * `/bitedisplays create <type> [name]`: spawns an entity where the player stands and selects it.
 * The rest of the command names an NPC, item, or block, and is the first line of text (required there).
 * Item and block displays show the held item.
 */
class CreateSubCommand : SubCommand("create", Permissions.USE) {

    override fun isAvailableTo(sender: CommandSender): Boolean {
        return super.isAvailableTo(sender) && BiteType.entries.any { canUse(sender, it) }
    }

    override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val player = sender as Player
        val type = args.firstOrNull()?.lowercase(Locale.ROOT)?.let { key -> BiteType.entries.firstOrNull { it.key == key } }
            ?: return sendUsage(player, label)
        if (!checkType(player, type)) return

        // Blank means no name
        val name = args.drop(1).joinToString(" ").takeIf { it.isNotBlank() }
        val id = UUID.randomUUID()
        val number = EntityManager.nextNumber(type)
        val location = EntityManager.placement(type, player.location)
        val held = player.inventory.itemInMainHand

        val entity: BiteEntity = when (type) {
            BiteType.NPC -> BiteNpc(id = id, number = number, location = location, name = name)
            BiteType.TEXT -> BiteText(id = id, number = number, location = location,
                lines = mutableListOf(name ?: return sendUsage(player, label, "create-text")),
                billboard = settings.defaultTextBillboard)
            BiteType.ITEM -> {
                if (held.type.isAir) return error(player, "no-item")
                BiteItem(id = id, number = number, location = location, itemStack = held.clone(), nameText = name)
            }
            BiteType.BLOCK -> {
                // Air counts as a block, so it's ruled out first
                if (held.type.isAir || !held.type.isBlock) return error(player, "no-block")
                BiteBlock(id = id, number = number, location = location, itemStack = held.clone(), nameText = name)
            }
        }

        if (EntityManager.spawn(entity) == null) return error(player, "spawn-failed", typePlaceholder(type))

        EntityManager.select(player.uniqueId, entity.id)
        success(player, "created", entityPlaceholders(entity))
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val input = args.last()
        if (args.size == 1) return matching(input, BiteType.entries.filter { canUse(sender, it) }.map { it.key })

        val type = BiteType.entries.firstOrNull { it.key == args[0].lowercase(Locale.ROOT) } ?: return emptyList()
        if (args.size != 2) return emptyList()
        return hint(input, if (type == BiteType.TEXT) "<text>" else "[name]")
    }

}
