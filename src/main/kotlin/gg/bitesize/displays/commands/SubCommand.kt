package gg.bitesize.displays.commands

import gg.bitesize.displays.config.Settings
import gg.bitesize.displays.managers.ConfigManager
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.managers.FormatManager
import gg.bitesize.displays.models.BiteEntity
import gg.bitesize.displays.models.BiteType
import gg.bitesize.displays.models.CommandRunner
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.BigDecimal

/**
 * A `/bitedisplays <name> ...` subcommand. The root command checks player-only,
 * permission, and feature toggles before calling [execute].
 */
abstract class SubCommand(
    val name: String,
    val permission: String,
    /** help.<key> lines shown for this subcommand */
    val helpKeys: List<String> = listOf(name),
    val feature: BiteType? = null,
    val playerOnly: Boolean = true,
) {

    protected val settings: Settings get() = ConfigManager.settings

    abstract fun execute(sender: CommandSender, label: String, args: List<String>)

    /** [args] excludes the subcommand name itself. */
    open fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()

    fun isAvailableTo(sender: CommandSender): Boolean {
        return (!playerOnly || sender is Player)
                && sender.hasPermission(permission)
                && (feature == null || settings.isEnabled(feature))
    }

    /** Sends `errors.<key>`. */
    protected fun error(sender: CommandSender, key: String, vararg placeholders: TagResolver) {
        FormatManager.error(sender, ConfigManager.getMessage("errors.$key"), *placeholders)
    }

    /** Sends `messages.<key>`. */
    protected fun success(sender: CommandSender, key: String, vararg placeholders: TagResolver) {
        FormatManager.send(sender, ConfigManager.getMessage("messages.$key"), true, *placeholders)
    }

    /** Sends `errors.usage-<usageKey>`. */
    protected fun sendUsage(sender: CommandSender, label: String, usageKey: String = name) {
        error(sender, "usage-$usageKey", labelPlaceholder(label))
    }

    protected fun labelPlaceholder(label: String): TagResolver = Placeholder.unparsed("label", label)

    protected fun matching(input: String, options: Iterable<String>): List<String> {
        return options.filter { it.startsWith(input, ignoreCase = true) }
    }

    /** A hint like `<name>` while the argument is still empty. */
    protected fun hint(input: String, hint: String): List<String> {
        return if (input.isEmpty()) listOf(hint) else emptyList()
    }

    /** The player's selected entity, or null after telling them how to select one. */
    protected fun selection(player: Player, label: String): BiteEntity? {
        return EntityManager.getSelection(player.uniqueId)
            ?: null.also { error(player, "no-selection", labelPlaceholder(label)) }
    }

    /** `types.<key>` from messages.yml, like "NPC" or "text display". */
    protected fun typeName(type: BiteType): String = ConfigManager.getMessage("types.${type.key}")

    protected fun typePlaceholder(type: BiteType): TagResolver = Placeholder.parsed("type", typeName(type))

    /** `<type>` and `<id>` of an entity. */
    protected fun entityPlaceholders(entity: BiteEntity): TagResolver {
        return TagResolver.resolver(typePlaceholder(entity.type), Placeholder.unparsed("id", entity.displayId))
    }

    /**
     * Text a player typed, formatted the way it shows in the world: MiniMessage or legacy codes.
     * Inserted as a finished component, so its tags can't spill into the rest of the message.
     */
    protected fun styledPlaceholder(key: String, text: String): TagResolver {
        return Placeholder.component(key, FormatManager.parseMiniMessage(text))
    }

    /** `<runner>`: who runs a click command, from the runners section of messages.yml. */
    protected fun runnerPlaceholder(runner: CommandRunner): TagResolver {
        return Placeholder.parsed("runner", ConfigManager.getMessage("runners.${runner.key}"))
    }

    /** Every entity ID, sorted by type and then number, for tab completion. */
    protected fun entityIds(): List<String> {
        return EntityManager.getAll().sortedWith(compareBy({ it.type }, { it.number })).map { it.displayId }
    }

    /** `<world>`, `<x>`, `<y>`, and `<z>` of a block position. */
    protected fun locationPlaceholders(location: Location): TagResolver {
        return TagResolver.resolver(
            Placeholder.unparsed("world", location.world?.name ?: "?"),
            Placeholder.unparsed("x", location.blockX.toString()),
            Placeholder.unparsed("y", location.blockY.toString()),
            Placeholder.unparsed("z", location.blockZ.toString()),
        )
    }

    /** A number without trailing zeros: 2.0 shows as 2, 0.50 as 0.5. */
    protected fun number(value: Number): String {
        return BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
    }

}
