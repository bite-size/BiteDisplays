package gg.bitesize.displays.commands

import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.storage.EntityDatabase
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.joml.Vector3f
import java.util.Locale

/**
 * `/bitedisplays <action> ...`, which edits the player's selection, whatever its type.
 * On a type outside [types] it says which types the action works on.
 * [T] is what those types share, like BiteDisplay for item and block, and [edit] gets the selection as one.
 */
abstract class EditSubCommand<T : BiteEntity>(name: String, vararg types: BiteType) : SubCommand(name, Permissions.USE) {

    val types: Set<BiteType> = types.toSet()

    private val booleans = listOf("true", "false")

    override fun isAvailableTo(sender: CommandSender): Boolean {
        return super.isAvailableTo(sender) && types.any { canUse(sender, it) }
    }

    /** Whether tab completion offers this: available, and works on the player's selection if they have one. */
    fun isSuggestedTo(sender: CommandSender): Boolean {
        if (!isAvailableTo(sender)) return false
        val selected = (sender as? Player)?.let { EntityManager.getSelection(it.uniqueId) } ?: return true
        return selected.type in types
    }

    final override fun execute(sender: CommandSender, label: String, args: List<String>) {
        val player = sender as Player
        val entity = selection(player, label) ?: return

        if (entity.type !in types) {
            return error(player, "unsupported",
                labelPlaceholder(label), Placeholder.unparsed("action", name), Placeholder.parsed("types", typeList()))
        }
        if (!checkType(player, entity.type)) return

        @Suppress("UNCHECKED_CAST")
        edit(player, entity as T, label, args)
        // Some edits (an NPC's click command or sound) don't respawn anything, so the save is flagged here too
        EntityDatabase.markDirty()
    }

    /** [args] excludes the action. */
    protected abstract fun edit(player: Player, entity: T, label: String, args: List<String>)

    final override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val player = sender as? Player ?: return emptyList()
        val entity = EntityManager.getSelection(player.uniqueId)?.takeIf { it.type in types } ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        return complete(entity as T, args)
    }

    /** Completions for the selection, which is one of [types]. [args] excludes the action. */
    protected open fun complete(entity: T, args: List<String>): List<String> = emptyList()

    /** The types this works on, like "NPCs, item displays and block displays", from `types-plural`. */
    private fun typeList(): String {
        val names = BiteType.entries.filter { it in types }.map { ConfigManager.getMessage("types-plural.${it.key}") }
        if (names.size == 1) return names[0]
        return names.dropLast(1).joinToString(", ") + ConfigManager.getMessage("types-plural.and") + names.last()
    }

    /** Parses `true` or `false`, or returns null after messaging the player. */
    protected fun parseToggle(player: Player, label: String, args: List<String>): Boolean? {
        val input = args.firstOrNull() ?: return null.also { sendUsage(player, label) }
        return input.lowercase(Locale.ROOT).toBooleanStrictOrNull()
            ?: null.also { error(player, "invalid-boolean") }
    }

    /** Parses a positive uniform scale, or returns null after messaging the player. */
    protected fun parseSize(player: Player, label: String, args: List<String>): Vector3f? {
        val input = args.firstOrNull() ?: return null.also { sendUsage(player, label) }
        val size = input.toFloatOrNull()?.takeIf { it > 0f && it.isFinite() }
            ?: return null.also { error(player, "invalid-size") }
        return Vector3f(size, size, size)
    }

    protected fun toggleCompletions(args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], booleans) else emptyList()
    }

}

/** `<action> <true|false>` for one setting of one type, answered with `messages.<action>-on` or `-off`. */
class ToggleSubCommand<T : BiteEntity>(name: String, type: BiteType, private val set: (T, Boolean) -> Unit)
    : EditSubCommand<T>(name, type) {

    override fun edit(player: Player, entity: T, label: String, args: List<String>) {
        val value = parseToggle(player, label, args) ?: return

        set(entity, value)
        EntityManager.updateEntity(entity)
        success(player, if (value) "$name-on" else "$name-off")
    }

    override fun complete(entity: T, args: List<String>): List<String> = toggleCompletions(args)

}
