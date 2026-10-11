package gg.bitesize.displays.commands.sub

import gg.bitesize.displays.commands.EditSubCommand
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteBlock
import gg.bitesize.displays.models.BiteDisplay
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteItem
import gg.bitesize.displays.models.BiteNpc
import gg.bitesize.displays.models.BiteText
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.models.ClickCommand
import gg.bitesize.displays.models.Clickable
import gg.bitesize.displays.models.CommandRunner
import gg.bitesize.displays.util.Colors
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import java.util.Locale

// Actions shared by more than one type, and the item and block ones. NPC and text ones are in NpcSubCommands and TextSubCommands.

/** `name [name]`: the NPC's nametag, or the label above an item or block. No name removes it. */
class NameSubCommand : EditSubCommand<BiteEntity>("name", BiteType.NPC, BiteType.ITEM, BiteType.BLOCK) {

    override fun edit(player: Player, entity: BiteEntity, label: String, args: List<String>) {
        val name = args.joinToString(" ").takeIf { it.isNotBlank() }
        when (entity) {
            is BiteNpc -> entity.name = name
            is BiteDisplay -> entity.nameText = name
        }
        EntityManager.updateEntity(entity)

        if (name == null) success(player, "name-removed") else success(player, "name-set", styledPlaceholder("name", name))
    }

    override fun complete(entity: BiteEntity, args: List<String>): List<String> {
        return if (args.size == 1) hint(args[0], "[name]") else emptyList()
    }

}

/** `cmd [<console|player> <command>]`, on every type: no arguments removes the command. */
class CommandSubCommand : EditSubCommand<Clickable>("cmd", *BiteType.entries.toTypedArray()) {

    private val runners = CommandRunner.entries.map { it.key }

    override fun edit(player: Player, entity: Clickable, label: String, args: List<String>) {
        if (args.isEmpty()) {
            apply(entity, null)
            success(player, "command-removed")
            return
        }

        val runner = CommandRunner.fromKey(args[0])
        if (runner == null || args.size < 2) return sendUsage(player, label)

        val click = ClickCommand(runner, args.drop(1).joinToString(" "))
        apply(entity, click)
        success(player, "command-set", Placeholder.unparsed("command", click.command), runnerPlaceholder(runner))
    }

    private fun apply(entity: Clickable, click: ClickCommand?) {
        entity.command = click
        // NPCs can always be clicked. Displays get a hitbox while they have a command.
        if (entity !is BiteNpc) EntityManager.updateEntity(entity)
    }

    override fun complete(entity: Clickable, args: List<String>): List<String> = when (args.size) {
        1 -> matching(args[0], runners)
        2 -> hint(args[1], "<command>")
        else -> emptyList()
    }

}

/** `size <scale>`, on every type. NPCs use the scale attribute, which has limits. */
class SizeSubCommand : EditSubCommand<BiteEntity>("size", *BiteType.entries.toTypedArray()) {

    override fun edit(player: Player, entity: BiteEntity, label: String, args: List<String>) {
        if (entity is BiteNpc) return npcSize(player, entity, label, args)

        val scale = parseSize(player, label, args) ?: return
        when (entity) {
            is BiteDisplay -> entity.scale = scale
            is BiteText -> entity.scale = scale
        }
        EntityManager.updateEntity(entity)
        sizeSet(player, scale.x.toDouble())
    }

    private fun npcSize(player: Player, npc: BiteNpc, label: String, args: List<String>) {
        val input = args.firstOrNull() ?: return sendUsage(player, label)
        val size = input.toDoubleOrNull()?.takeIf { it in BiteNpc.MIN_SCALE..BiteNpc.MAX_SCALE }
            ?: return error(player, "invalid-npc-size",
                Placeholder.unparsed("min", number(BiteNpc.MIN_SCALE)), Placeholder.unparsed("max", number(BiteNpc.MAX_SCALE)))

        npc.scale = size
        EntityManager.updateEntity(npc)
        sizeSet(player, size)
    }

    private fun sizeSet(player: Player, size: Double) {
        success(player, "size-set", Placeholder.unparsed("size", number(size)))
    }

    override fun complete(entity: BiteEntity, args: List<String>): List<String> {
        return if (args.size == 1) hint(args[0], "<scale>") else emptyList()
    }

}

/**
 * `glow [color]`: no color or `none` turns it off. Text displays can't glow (the client draws no outline for them).
 * NPCs glow through scoreboard teams, which only know the named chat colors. Displays also take hex.
 */
class GlowSubCommand : EditSubCommand<BiteEntity>("glow", BiteType.NPC, BiteType.ITEM, BiteType.BLOCK) {

    private val options = listOf(NO_GLOW) + Colors.GLOW_COLORS

    override fun edit(player: Player, entity: BiteEntity, label: String, args: List<String>) {
        val color = args.firstOrNull()?.lowercase(Locale.ROOT)?.takeUnless { it == NO_GLOW }

        if (color != null) {
            val valid = if (entity is BiteNpc) Colors.teamColor(color) != null else Colors.parse(color) != null
            if (!valid) return error(player, if (entity is BiteNpc) "invalid-team-color" else "invalid-glow-color")
        }

        when (entity) {
            is BiteNpc -> entity.glowColor = color
            is BiteDisplay -> entity.glowColor = color
        }
        EntityManager.updateEntity(entity)

        if (color == null) success(player, "glow-removed") else success(player, "glow-set", Placeholder.unparsed("color", color))
    }

    override fun complete(entity: BiteEntity, args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], options) else emptyList()
    }

    private companion object {
        const val NO_GLOW = "none"
    }

}

/** `rotate <yaw|pitch> <degrees>` for item and block displays. */
class RotateSubCommand : EditSubCommand<BiteDisplay>("rotate", BiteType.ITEM, BiteType.BLOCK) {

    private val axes = listOf("yaw", "pitch")

    override fun edit(player: Player, entity: BiteDisplay, label: String, args: List<String>) {
        val axis = args.getOrNull(0)?.lowercase(Locale.ROOT)
        if (axis == null || axis !in axes || args.size < 2) return sendUsage(player, label)

        val degrees = args[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return error(player, "invalid-number")

        if (axis == "yaw") entity.yaw = degrees else entity.pitch = degrees
        EntityManager.updateEntity(entity)
        success(player, "rotated", Placeholder.unparsed("axis", axis), Placeholder.unparsed("degrees", number(degrees)))
    }

    override fun complete(entity: BiteDisplay, args: List<String>): List<String> = when (args.size) {
        1 -> matching(args[0], axes)
        2 -> hint(args[1], "<degrees>")
        else -> emptyList()
    }

}

/** `setblock`: shows the block in the player's main hand instead, keeping everything else. */
class SetBlockSubCommand : EditSubCommand<BiteBlock>("setblock", BiteType.BLOCK) {

    override fun edit(player: Player, entity: BiteBlock, label: String, args: List<String>) {
        val held = player.inventory.itemInMainHand
        // Air counts as a block, so it's ruled out first
        if (held.type.isAir || !held.type.isBlock) return error(player, "no-block")

        entity.itemStack = held.clone()
        EntityManager.updateEntity(entity)
        success(player, "block-set", Placeholder.unparsed("material", held.type.name.lowercase(Locale.ROOT)))
    }

}

/** `setitem`: shows the item in the player's main hand instead, keeping everything else. */
class SetItemSubCommand : EditSubCommand<BiteItem>("setitem", BiteType.ITEM) {

    override fun edit(player: Player, entity: BiteItem, label: String, args: List<String>) {
        val held = player.inventory.itemInMainHand
        if (held.type.isAir) return error(player, "no-item")

        entity.itemStack = held.clone()
        EntityManager.updateEntity(entity)
        success(player, "item-set", Placeholder.unparsed("material", held.type.name.lowercase(Locale.ROOT)))
    }

}
