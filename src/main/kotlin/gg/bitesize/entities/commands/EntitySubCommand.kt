package gg.bitesize.entities.commands

import gg.bitesize.entities.managers.ConfigManager
import gg.bitesize.entities.managers.EntityManager
import gg.bitesize.entities.managers.FormatManager
import gg.bitesize.entities.models.BiteEntity
import gg.bitesize.entities.models.BiteType
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.joml.Vector3f
import java.util.Locale

/**
 * `/biteentities <type> <action> ...` for one entity type, like `npc create`.
 * Most actions edit the player's selection, which must be a [type].
 * Running it without an action lists every action from `help.<type>-<action>`.
 */
abstract class EntitySubCommand<T : BiteEntity>(
    val type: BiteType,
    permission: String,
    /** Declaration order is the help and tab-completion order */
    val actions: List<String>,
) : SubCommand(type.key, permission, feature = type, playerOnly = true) {

    private val booleans = listOf("true", "false")

    final override fun execute(sender: CommandSender, label: String, args: List<String>) {
        when (val action = args.firstOrNull()?.lowercase(Locale.ROOT)) {
            null, "help" -> sendActionHelp(sender, label)
            in actions -> execute(sender as Player, action, label, args.drop(1))
            else -> sendUsage(sender, label)
        }
    }

    /** [args] excludes the action. */
    protected abstract fun execute(player: Player, action: String, label: String, args: List<String>)

    final override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size <= 1) return matching(args.firstOrNull().orEmpty(), actions)

        val action = args[0].lowercase(Locale.ROOT)
        if (action !in actions) return emptyList()

        return tabComplete(sender, action, args.drop(1))
    }

    /** [args] excludes the action. */
    protected open fun tabComplete(sender: CommandSender, action: String, args: List<String>): List<String> {
        return emptyList()
    }

    /** The player's selection if it's a [type], or null after messaging them. */
    @Suppress("UNCHECKED_CAST")
    protected fun selected(player: Player, label: String): T? {
        val entity = selection(player, label) ?: return null

        if (entity.type != type) {
            error(player, "wrong-type", typePlaceholder(type))
            return null
        }

        return entity as T
    }

    /** The player's selection if it's a [type], for tab completion. */
    @Suppress("UNCHECKED_CAST")
    protected fun selectedOrNull(sender: CommandSender): T? {
        val player = sender as? Player ?: return null
        return EntityManager.getSelection(player.uniqueId)?.takeIf { it.type == type } as T?
    }

    /** Spawns a new entity where the player stands and selects it. */
    protected fun spawnAndSelect(player: Player, entity: T) {
        if (EntityManager.spawn(entity) == null) {
            error(player, "spawn-failed", typePlaceholder(type))
            return
        }

        EntityManager.select(player.uniqueId, entity.id)
        success(player, "created", entityPlaceholders(entity))
    }

    /** Sends `errors.usage-<type>-<action>`. */
    protected fun sendActionUsage(player: Player, label: String, action: String) {
        sendUsage(player, label, "$name-$action")
    }

    /** Parses `true` or `false`, or returns null after messaging the player. */
    protected fun parseToggle(player: Player, label: String, action: String, args: List<String>): Boolean? {
        val input = args.firstOrNull() ?: return null.also { sendActionUsage(player, label, action) }
        return input.lowercase(Locale.ROOT).toBooleanStrictOrNull()
            ?: null.also { error(player, "invalid-boolean") }
    }

    /** Parses a positive uniform scale, or returns null after messaging the player. */
    protected fun parseSize(player: Player, label: String, action: String, args: List<String>): Vector3f? {
        val input = args.firstOrNull() ?: return null.also { sendActionUsage(player, label, action) }
        val size = input.toFloatOrNull()?.takeIf { it > 0f && it.isFinite() }
            ?: return null.also { error(player, "invalid-size") }
        return Vector3f(size, size, size)
    }

    protected fun sizeSet(player: Player, scale: Vector3f) {
        success(player, "size-set", Placeholder.unparsed("size", number(scale.x)))
    }

    protected fun toggleCompletions(args: List<String>): List<String> {
        return if (args.size == 1) matching(args[0], booleans) else emptyList()
    }

    private fun sendActionHelp(sender: CommandSender, label: String) {
        val labelPlaceholder = labelPlaceholder(label)

        FormatManager.send(sender, ConfigManager.getMessage("help.$name-header"), true, labelPlaceholder)
        actions.forEach { FormatManager.send(sender, ConfigManager.getMessage("help.$name-$it"), false, labelPlaceholder) }
        FormatManager.send(sender, ConfigManager.getMessage("help.optional-note"), false, labelPlaceholder)
    }

}
