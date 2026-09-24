@file:Suppress("UnstableApiUsage")
package gg.bitesize.plugin.commands

import gg.bitesize.plugin.gui.EquipmentGui
import gg.bitesize.plugin.managers.EntityManager
import gg.bitesize.plugin.managers.FormatManager
import gg.bitesize.plugin.models.*
import gg.bitesize.plugin.storage.EntityDatabase
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.joml.Vector3f
import java.util.UUID

class EntityCommand(private val plugin: JavaPlugin) : BasicCommand {

    private val rootSubs = listOf("npc", "item", "block", "text", "select", "movehere", "remove", "info", "list", "save", "reload")
    private val npcSubs = listOf("create", "skin", "cmd", "name", "glow", "desc", "equipment", "sound")
    private val itemSubs = listOf("create", "rotate", "size", "name", "cmd", "glowing", "bouncing")
    private val blockSubs = listOf("create", "rotate", "size", "name", "cmd", "glowing")
    private val textSubs = listOf("create", "background", "addline", "delline", "editline", "glow", "billboard", "size")
    private val rotateAxes = listOf("yaw", "pitch")
    private val booleans = listOf("true", "false")

    // ── Dispatch ───────────────────────────────────────────────────

    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        val sender = stack.sender
        if (args.isEmpty()) { sendUsage(sender); return }

        when (args[0].lowercase()) {
            "npc"     -> handleNpc(sender, args)
            "item"    -> handleItem(sender, args)
            "block"   -> handleBlock(sender, args)
            "text"    -> handleText(sender, args)
            "select"  -> handleSelect(sender)
            "movehere"-> handleMoveHere(sender)
            "remove"  -> handleRemove(sender)
            "info"    -> handleInfo(sender)
            "list"    -> handleList(sender, args)
            "save"    -> handleSave(sender)
            "reload"  -> handleReload(sender)
            else      -> sendUsage(sender)
        }
    }

    override fun suggest(stack: CommandSourceStack, args: Array<String>): Collection<String> {
        return when (args.size) {
            1 -> rootSubs.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "npc"   -> npcSubs.filter { it.startsWith(args[1], ignoreCase = true) }
                "item"  -> itemSubs.filter { it.startsWith(args[1], ignoreCase = true) }
                "block" -> blockSubs.filter { it.startsWith(args[1], ignoreCase = true) }
                "text"  -> textSubs.filter { it.startsWith(args[1], ignoreCase = true) }
                "list"  -> listOf("npc", "item", "block", "text").filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            3 -> when {
                args[0].eq("npc") && args[1].eq("glow") -> FormatManager.GLOW_COLORS.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("npc") && args[1].eq("sound") -> Registry.SOUNDS.map { it.key().value() }.filter { it.contains(args[2], ignoreCase = true) }
                args[0].eq("item") && args[1].eq("rotate") -> rotateAxes.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("item") && args[1].eq("glowing") -> booleans.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("item") && args[1].eq("bouncing") -> booleans.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("block") && args[1].eq("rotate") -> rotateAxes.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("block") && args[1].eq("glowing") -> booleans.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("text") && args[1].eq("glow") -> FormatManager.GLOW_COLORS.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].eq("text") && args[1].eq("billboard") -> booleans.filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    override fun canUse(sender: CommandSender): Boolean = sender.hasPermission("biteentities.admin")
    override fun permission(): String = "biteentities.admin"

    private fun String.eq(other: String) = this.equals(other, ignoreCase = true)

    // ══════════════════════════════════════════════════════════════
    //  NPC COMMANDS
    // ══════════════════════════════════════════════════════════════

    private fun handleNpc(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) { sender.sendMessage(FormatManager.formatMessage("Usage: /be npc <${npcSubs.joinToString("|")}>")); return }
        when (args[1].lowercase()) {
            "create"    -> handleNpcCreate(sender, args)
            "skin"      -> handleNpcSkin(sender, args)
            "cmd"       -> handleNpcCmd(sender, args)
            "name"      -> handleNpcName(sender, args)
            "glow"      -> handleNpcGlow(sender, args)
            "desc"      -> handleNpcDesc(sender, args)
            "equipment" -> handleNpcEquipment(sender)
            "sound"     -> handleNpcSound(sender, args)
            else        -> sender.sendMessage(FormatManager.formatMessage("Usage: /be npc <${npcSubs.joinToString("|")}>"))
        }
    }

    private fun handleNpcCreate(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be npc create <name>")); return }

        val name = args.drop(2).joinToString(" ")
        val id = UUID.randomUUID()
        val entity = BiteNpc(id = id, location = player.location, name = name)

        if (EntityManager.spawn(entity) != null) {
            EntityManager.select(player.uniqueId, id)
            player.sendMessage(FormatManager.formatMessageWithValue("Created NPC:", name))
        } else {
            player.sendMessage(FormatManager.formatMessage("Failed to spawn NPC."))
        }
    }

    private fun handleNpcSkin(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be npc skin <player_name>")); return }

        val targetName = args[2]
        player.sendMessage(FormatManager.formatMessage("Fetching skin for $targetName..."))

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                val profile = Bukkit.createProfile(targetName)
                profile.complete()
                val textures = profile.properties.firstOrNull { it.name == "textures" }

                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (textures != null) {
                        npc.skinValue = textures.value
                        npc.skinSignature = textures.signature
                        EntityManager.updateEntity(npc)
                        player.sendMessage(FormatManager.formatMessageWithValue("Skin applied:", targetName))
                    } else {
                        player.sendMessage(FormatManager.formatMessage("Could not fetch skin for $targetName."))
                    }
                })
            } catch (e: Exception) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    player.sendMessage(FormatManager.formatMessage("Failed to fetch skin: ${e.message}"))
                })
            }
        })
    }

    private fun handleNpcCmd(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return

        if (args.size < 3) {
            npc.command = null
            player.sendMessage(FormatManager.formatMessage("Click command removed."))
            return
        }

        val cmd = args.drop(2).joinToString(" ")
        npc.command = cmd
        player.sendMessage(FormatManager.formatMessageWithValue("Click command set:", cmd))
    }

    private fun handleNpcName(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return

        if (args.size < 3) {
            npc.name = null
            EntityManager.updateEntity(npc)
            player.sendMessage(FormatManager.formatMessage("Name removed."))
            return
        }

        val name = args.drop(2).joinToString(" ")
        npc.name = name
        EntityManager.updateEntity(npc)
        player.sendMessage(FormatManager.formatMessageWithValue("Name set:", name))
    }

    private fun handleNpcGlow(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return

        if (args.size < 3) {
            npc.glowColor = null
            EntityManager.updateEntity(npc)
            player.sendMessage(FormatManager.formatMessage("Glow removed."))
            return
        }

        npc.glowColor = args[2]
        EntityManager.updateEntity(npc)
        player.sendMessage(FormatManager.formatMessageWithValue("Glow set:", args[2]))
    }

    private fun handleNpcDesc(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return

        if (args.size < 3) {
            npc.description = null
            EntityManager.updateEntity(npc)
            player.sendMessage(FormatManager.formatMessage("Description removed."))
            return
        }

        val desc = args.drop(2).joinToString(" ")
        npc.description = desc
        EntityManager.updateEntity(npc)
        player.sendMessage(FormatManager.formatMessageWithValue("Description set:", desc))
    }

    private fun handleNpcEquipment(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return
        EquipmentGui.open(player, npc)
    }

    private fun handleNpcSound(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val npc = requireSelected<BiteNpc>(player, "NPC") ?: return

        if (args.size < 3) {
            npc.sound = null
            player.sendMessage(FormatManager.formatMessage("Click sound removed."))
            return
        }

        val soundKey = args[2].lowercase()
        // Validate the sound exists in the registry
        val sound = Registry.SOUNDS.firstOrNull { it.key().value() == soundKey }
        if (sound == null) {
            player.sendMessage(FormatManager.formatMessage("Unknown sound: $soundKey"))
            return
        }

        npc.sound = soundKey
        player.sendMessage(FormatManager.formatMessageWithValue("Click sound set:", soundKey))
    }

    // ══════════════════════════════════════════════════════════════
    //  ITEM COMMANDS
    // ══════════════════════════════════════════════════════════════

    private fun handleItem(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) { sender.sendMessage(FormatManager.formatMessage("Usage: /be item <${itemSubs.joinToString("|")}>")); return }
        when (args[1].lowercase()) {
            "create"  -> handleItemCreate(sender)
            "rotate"  -> handleDisplayRotate<BiteItem>(sender, args, "Item")
            "size"    -> handleDisplaySize<BiteItem>(sender, args, "Item")
            "name"    -> handleDisplayName<BiteItem>(sender, args, "Item")
            "cmd"     -> handleDisplayCmd<BiteItem>(sender, args, "Item")
            "glowing" -> handleDisplayGlowing<BiteItem>(sender, args, "Item")
            "bouncing"-> handleItemBouncing(sender, args)
            else      -> sender.sendMessage(FormatManager.formatMessage("Usage: /be item <${itemSubs.joinToString("|")}>"))
        }
    }

    private fun handleItemCreate(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) {
            player.sendMessage(FormatManager.formatMessage("Hold an item to create an item display."))
            return
        }

        val id = UUID.randomUUID()
        val entity = BiteItem(id = id, location = player.location, itemStack = item.clone())

        if (EntityManager.spawn(entity) != null) {
            EntityManager.select(player.uniqueId, id)
            player.sendMessage(FormatManager.formatMessageWithValue("Created item display:", id.toString().substring(0, 8)))
        } else {
            player.sendMessage(FormatManager.formatMessage("Failed to spawn item display."))
        }
    }

    private fun handleItemBouncing(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val item = requireSelected<BiteItem>(player, "Item") ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be item bouncing <true|false>")); return }

        val value = args[2].toBooleanStrictOrNull()
        if (value == null) { player.sendMessage(FormatManager.formatMessage("Value must be true or false.")); return }

        item.bouncing = value
        EntityManager.updateEntity(item)
        player.sendMessage(FormatManager.formatMessageWithValue("Bouncing:", value.toString()))
    }

    // ══════════════════════════════════════════════════════════════
    //  BLOCK COMMANDS
    // ══════════════════════════════════════════════════════════════

    private fun handleBlock(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) { sender.sendMessage(FormatManager.formatMessage("Usage: /be block <${blockSubs.joinToString("|")}>")); return }
        when (args[1].lowercase()) {
            "create"  -> handleBlockCreate(sender)
            "rotate"  -> handleDisplayRotate<BiteBlock>(sender, args, "Block")
            "size"    -> handleDisplaySize<BiteBlock>(sender, args, "Block")
            "name"    -> handleDisplayName<BiteBlock>(sender, args, "Block")
            "cmd"     -> handleDisplayCmd<BiteBlock>(sender, args, "Block")
            "glowing" -> handleDisplayGlowing<BiteBlock>(sender, args, "Block")
            else      -> sender.sendMessage(FormatManager.formatMessage("Usage: /be block <${blockSubs.joinToString("|")}>"))
        }
    }

    private fun handleBlockCreate(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR || !item.type.isBlock) {
            player.sendMessage(FormatManager.formatMessage("Hold a block to create a block display."))
            return
        }

        val id = UUID.randomUUID()
        val entity = BiteBlock(id = id, location = player.location, itemStack = item.clone())

        if (EntityManager.spawn(entity) != null) {
            EntityManager.select(player.uniqueId, id)
            player.sendMessage(FormatManager.formatMessageWithValue("Created block display:", id.toString().substring(0, 8)))
        } else {
            player.sendMessage(FormatManager.formatMessage("Failed to spawn block display."))
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SHARED DISPLAY COMMANDS (Item + Block)
    // ══════════════════════════════════════════════════════════════

    private inline fun <reified T : BiteEntity> handleDisplayRotate(sender: CommandSender, args: Array<out String>, label: String) {
        val player = requirePlayer(sender) ?: return
        val entity = requireSelected<T>(player, label) ?: return
        if (args.size < 4) { player.sendMessage(FormatManager.formatMessage("Usage: /be ${label.lowercase()} rotate <yaw|pitch> <value>")); return }

        val axis = args[2].lowercase()
        if (axis != "yaw" && axis != "pitch") { player.sendMessage(FormatManager.formatMessage("Axis must be yaw or pitch.")); return }
        val value = args[3].toFloatOrNull()
        if (value == null) { player.sendMessage(FormatManager.formatMessage("Value must be a number.")); return }

        when (entity) {
            is BiteBlock -> if (axis == "yaw") entity.yaw = value else entity.pitch = value
            is BiteItem  -> if (axis == "yaw") entity.yaw = value else entity.pitch = value
        }

        EntityManager.updateEntity(entity)
        player.sendMessage(FormatManager.formatMessageWithValue("Set $axis to:", "$value°"))
    }

    private inline fun <reified T : BiteEntity> handleDisplaySize(sender: CommandSender, args: Array<out String>, label: String) {
        val player = requirePlayer(sender) ?: return
        val entity = requireSelected<T>(player, label) ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be ${label.lowercase()} size <value>")); return }

        val value = args[2].toFloatOrNull()
        if (value == null || value <= 0f) { player.sendMessage(FormatManager.formatMessage("Size must be a positive number.")); return }

        val scale = Vector3f(value, value, value)
        when (entity) {
            is BiteBlock -> entity.scale = scale
            is BiteItem  -> entity.scale = scale
            is BiteText  -> entity.scale = scale
        }

        EntityManager.updateEntity(entity)
        player.sendMessage(FormatManager.formatMessageWithValue("Size set:", value.toString()))
    }

    private inline fun <reified T : BiteEntity> handleDisplayName(sender: CommandSender, args: Array<out String>, label: String) {
        val player = requirePlayer(sender) ?: return
        val entity = requireSelected<T>(player, label) ?: return

        if (args.size < 3) {
            // Remove name
            when (entity) {
                is BiteBlock -> entity.nameText = null
                is BiteItem  -> entity.nameText = null
            }
            EntityManager.updateEntity(entity)
            player.sendMessage(FormatManager.formatMessage("Name removed."))
            return
        }

        val name = args.drop(2).joinToString(" ")
        when (entity) {
            is BiteBlock -> entity.nameText = name
            is BiteItem  -> entity.nameText = name
        }
        EntityManager.updateEntity(entity)
        player.sendMessage(FormatManager.formatMessageWithValue("Name set:", name))
    }

    private inline fun <reified T : BiteEntity> handleDisplayCmd(sender: CommandSender, args: Array<out String>, label: String) {
        val player = requirePlayer(sender) ?: return
        val entity = requireSelected<T>(player, label) ?: return

        if (args.size < 3) {
            when (entity) {
                is BiteBlock -> entity.command = null
                is BiteItem  -> entity.command = null
            }
            EntityManager.updateEntity(entity)
            player.sendMessage(FormatManager.formatMessage("Click command removed."))
            return
        }

        val cmd = args.drop(2).joinToString(" ")
        when (entity) {
            is BiteBlock -> entity.command = cmd
            is BiteItem  -> entity.command = cmd
        }
        EntityManager.updateEntity(entity)
        player.sendMessage(FormatManager.formatMessageWithValue("Click command set:", cmd))
    }

    private inline fun <reified T : BiteEntity> handleDisplayGlowing(sender: CommandSender, args: Array<out String>, label: String) {
        val player = requirePlayer(sender) ?: return
        val entity = requireSelected<T>(player, label) ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be ${label.lowercase()} glowing <true|false>")); return }

        val value = args[2].toBooleanStrictOrNull()
        if (value == null) { player.sendMessage(FormatManager.formatMessage("Value must be true or false.")); return }

        when (entity) {
            is BiteBlock -> entity.glowing = value
            is BiteItem  -> entity.glowing = value
        }

        EntityManager.updateEntity(entity)
        player.sendMessage(FormatManager.formatMessageWithValue("Glowing:", value.toString()))
    }

    // ══════════════════════════════════════════════════════════════
    //  TEXT COMMANDS
    // ══════════════════════════════════════════════════════════════

    private fun handleText(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) { sender.sendMessage(FormatManager.formatMessage("Usage: /be text <${textSubs.joinToString("|")}>")); return }
        when (args[1].lowercase()) {
            "create"     -> handleTextCreate(sender, args)
            "background" -> handleTextBackground(sender, args)
            "addline"    -> handleTextAddLine(sender, args)
            "delline"    -> handleTextDelLine(sender, args)
            "editline"   -> handleTextEditLine(sender, args)
            "glow"       -> handleTextGlow(sender, args)
            "billboard"  -> handleTextBillboard(sender, args)
            "size"       -> handleDisplaySize<BiteText>(sender, args, "Text")
            else         -> sender.sendMessage(FormatManager.formatMessage("Usage: /be text <${textSubs.joinToString("|")}>"))
        }
    }

    private fun handleTextCreate(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val initialLine = if (args.size >= 3) args.drop(2).joinToString(" ") else "Text Display"
        val id = UUID.randomUUID()
        val entity = BiteText(id = id, location = player.location, lines = mutableListOf(initialLine))

        if (EntityManager.spawn(entity) != null) {
            EntityManager.select(player.uniqueId, id)
            player.sendMessage(FormatManager.formatMessageWithValue("Created text display:", id.toString().substring(0, 8)))
        } else {
            player.sendMessage(FormatManager.formatMessage("Failed to spawn text display."))
        }
    }

    private fun handleTextBackground(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val text = requireSelected<BiteText>(player, "Text") ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be text background <#RRGGBB|#AARRGGBB>")); return }

        val color = FormatManager.parseHexColor(args[2])
        if (color == null) { player.sendMessage(FormatManager.formatMessage("Invalid color. Use #RRGGBB or #AARRGGBB.")); return }

        text.backgroundColor = args[2]
        EntityManager.updateEntity(text)
        player.sendMessage(FormatManager.formatMessageWithValue("Background set:", args[2]))
    }

    private fun handleTextAddLine(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val text = requireSelected<BiteText>(player, "Text") ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be text addline <text>")); return }

        val line = args.drop(2).joinToString(" ")
        text.lines.add(line)
        EntityManager.updateEntity(text)
        player.sendMessage(FormatManager.formatMessageWithValue("Added line ${text.lines.size}:", line))
    }

    private fun handleTextDelLine(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val text = requireSelected<BiteText>(player, "Text") ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be text delline <number>")); return }

        val lineNum = args[2].toIntOrNull()
        if (lineNum == null || lineNum < 1 || lineNum > text.lines.size) {
            player.sendMessage(FormatManager.formatMessage("Invalid line number. Lines: 1-${text.lines.size}"))
            return
        }

        val removed = text.lines.removeAt(lineNum - 1)
        EntityManager.updateEntity(text)
        player.sendMessage(FormatManager.formatMessageWithValue("Removed line $lineNum:", removed))
    }

    private fun handleTextEditLine(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val text = requireSelected<BiteText>(player, "Text") ?: return
        if (args.size < 4) { player.sendMessage(FormatManager.formatMessage("Usage: /be text editline <number> <text>")); return }

        val lineNum = args[2].toIntOrNull()
        if (lineNum == null || lineNum < 1 || lineNum > text.lines.size) {
            player.sendMessage(FormatManager.formatMessage("Invalid line number. Lines: 1-${text.lines.size}"))
            return
        }

        val newContent = args.drop(3).joinToString(" ")
        text.lines[lineNum - 1] = newContent
        EntityManager.updateEntity(text)
        player.sendMessage(FormatManager.formatMessageWithValue("Edited line $lineNum:", newContent))
    }

    private fun handleTextGlow(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val text = requireSelected<BiteText>(player, "Text") ?: return

        if (args.size < 3) {
            text.glowColor = null
            EntityManager.updateEntity(text)
            player.sendMessage(FormatManager.formatMessage("Glow removed."))
            return
        }

        text.glowColor = args[2]
        EntityManager.updateEntity(text)
        player.sendMessage(FormatManager.formatMessageWithValue("Glow set:", args[2]))
    }

    private fun handleTextBillboard(sender: CommandSender, args: Array<out String>) {
        val player = requirePlayer(sender) ?: return
        val text = requireSelected<BiteText>(player, "Text") ?: return
        if (args.size < 3) { player.sendMessage(FormatManager.formatMessage("Usage: /be text billboard <true|false>")); return }

        val value = args[2].toBooleanStrictOrNull()
        if (value == null) { player.sendMessage(FormatManager.formatMessage("Value must be true or false.")); return }

        text.billboard = value
        EntityManager.updateEntity(text)
        player.sendMessage(FormatManager.formatMessageWithValue("Billboard:", value.toString()))
    }

    // ══════════════════════════════════════════════════════════════
    //  GLOBAL COMMANDS
    // ══════════════════════════════════════════════════════════════

    private fun handleSelect(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val result = player.world.rayTraceEntities(player.eyeLocation, player.eyeLocation.direction, 5.0, 0.5) { it.uniqueId != player.uniqueId }
        val hitEntity = result?.hitEntity

        if (hitEntity == null) {
            player.sendMessage(FormatManager.formatMessage("No entity found. Look at a BiteEntity within 5 blocks."))
            return
        }

        val biteEntity = EntityManager.findByBukkitId(hitEntity.uniqueId)
        if (biteEntity == null) {
            player.sendMessage(FormatManager.formatMessage("That entity is not managed by BiteEntities."))
            return
        }

        EntityManager.select(player.uniqueId, biteEntity.id)
        val type = entityTypeName(biteEntity)
        player.sendMessage(FormatManager.formatMessageWithValue("Selected $type:", biteEntity.id.toString().substring(0, 8)))
    }

    private fun handleMoveHere(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val entity = EntityManager.getSelection(player.uniqueId)
        if (entity == null) { player.sendMessage(FormatManager.formatMessage("No entity selected. Use /be select first.")); return }

        EntityManager.teleport(entity.id, player.location)
        player.sendMessage(FormatManager.formatMessageWithValue("Moved entity to your location:", entity.id.toString().substring(0, 8)))
    }

    private fun handleRemove(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val entity = EntityManager.getSelection(player.uniqueId)
        if (entity == null) { player.sendMessage(FormatManager.formatMessage("No entity selected. Use /be select first.")); return }

        EntityManager.despawn(entity.id)
        player.sendMessage(FormatManager.formatMessageWithValue("Removed:", entity.id.toString().substring(0, 8)))
    }

    private fun handleInfo(sender: CommandSender) {
        val player = requirePlayer(sender) ?: return
        val entity = EntityManager.getSelection(player.uniqueId)
        if (entity == null) { player.sendMessage(FormatManager.formatMessage("No entity selected. Use /be select first.")); return }

        val loc = entity.location
        val type = entityTypeName(entity)
        player.sendMessage(FormatManager.formatMessageWithValue("Entity Info:", entity.id.toString().substring(0, 8)))
        player.sendMessage(FormatManager.formatMessage(" Type: $type", prefix = false))
        player.sendMessage(FormatManager.formatMessage(" Location: ${loc.world?.name} (${loc.blockX}, ${loc.blockY}, ${loc.blockZ})", prefix = false))

        when (entity) {
            is BiteNpc -> {
                player.sendMessage(FormatManager.formatMessage(" Name: ${entity.name ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Skin: ${if (entity.skinValue != null) "set" else "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Command: ${entity.command ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Glow: ${entity.glowColor ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Description: ${entity.description ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Sound: ${entity.sound ?: "none"}", prefix = false))
            }
            is BiteBlock -> {
                player.sendMessage(FormatManager.formatMessage(" Block: ${entity.itemStack.type}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Scale: ${entity.scale.x}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Rotation: yaw=${entity.yaw} pitch=${entity.pitch}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Name: ${entity.nameText ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Command: ${entity.command ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Glowing: ${entity.glowing}", prefix = false))
            }
            is BiteItem -> {
                player.sendMessage(FormatManager.formatMessage(" Item: ${entity.itemStack.type}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Scale: ${entity.scale.x}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Rotation: yaw=${entity.yaw} pitch=${entity.pitch}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Name: ${entity.nameText ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Command: ${entity.command ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Glowing: ${entity.glowing}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Bouncing: ${entity.bouncing}", prefix = false))
            }
            is BiteText -> {
                player.sendMessage(FormatManager.formatMessage(" Lines: ${entity.lines.size}", prefix = false))
                entity.lines.forEachIndexed { i, line ->
                    player.sendMessage(FormatManager.formatMessage("  ${i + 1}: $line", prefix = false))
                }
                player.sendMessage(FormatManager.formatMessage(" Background: ${entity.backgroundColor ?: "default"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Glow: ${entity.glowColor ?: "none"}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Billboard: ${entity.billboard}", prefix = false))
                player.sendMessage(FormatManager.formatMessage(" Scale: ${entity.scale.x}", prefix = false))
            }
        }
    }

    private fun handleList(sender: CommandSender, args: Array<out String>) {
        val filter = if (args.size >= 2) args[1].lowercase() else null
        val entities = EntityManager.getAll().filter { entity ->
            when (filter) {
                "npc" -> entity is BiteNpc
                "block" -> entity is BiteBlock
                "item" -> entity is BiteItem
                "text" -> entity is BiteText
                else -> true
            }
        }

        if (entities.isEmpty()) {
            sender.sendMessage(FormatManager.formatMessage("No entities found."))
            return
        }

        sender.sendMessage(FormatManager.formatMessageWithValue("Entities:", "(${entities.size})"))
        for (entity in entities) {
            val type = entityTypeName(entity)
            val shortId = entity.id.toString().substring(0, 8)
            val loc = entity.location
            val extra = when (entity) {
                is BiteNpc -> entity.name ?: ""
                is BiteBlock -> entity.itemStack.type.toString()
                is BiteItem -> entity.itemStack.type.toString()
                is BiteText -> "${entity.lines.size} lines"
                else -> ""
            }
            sender.sendMessage(FormatManager.formatMessage(
                " $type $shortId — $extra @ ${loc.world?.name} (${loc.blockX}, ${loc.blockY}, ${loc.blockZ})",
                prefix = false
            ))
        }
    }

    private fun handleSave(sender: CommandSender) {
        EntityDatabase.save()
        sender.sendMessage(FormatManager.formatMessageWithValue("Saved", "${EntityManager.getAll().size} entities."))
    }

    private fun handleReload(sender: CommandSender) {
        EntityManager.despawnAll()
        EntityDatabase.load()
        sender.sendMessage(FormatManager.formatMessageWithValue("Reloaded", "${EntityManager.getAll().size} entities."))
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private fun requirePlayer(sender: CommandSender): Player? {
        if (sender !is Player) {
            sender.sendMessage(FormatManager.formatMessage(FormatManager.PLAYER_ONLY))
            return null
        }
        return sender
    }

    private inline fun <reified T : BiteEntity> requireSelected(player: Player, typeName: String): T? {
        val entity = EntityManager.getSelection(player.uniqueId)
        if (entity == null) {
            player.sendMessage(FormatManager.formatMessage("No entity selected. Use /be select first."))
            return null
        }
        if (entity !is T) {
            player.sendMessage(FormatManager.formatMessage("Selected entity is not a $typeName."))
            return null
        }
        return entity
    }

    private fun entityTypeName(entity: BiteEntity): String = when (entity) {
        is BiteNpc -> "NPC"
        is BiteBlock -> "Block"
        is BiteItem -> "Item"
        is BiteText -> "Text"
        else -> "Unknown"
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(FormatManager.formatMessage("BiteEntities Commands:"))
        sender.sendMessage(FormatManager.formatMessage(" /be npc <create|skin|cmd|name|glow|desc|equipment|sound>", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be item <create|rotate|size|name|cmd|glowing|bouncing>", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be block <create|rotate|size|name|cmd|glowing>", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be text <create|background|addline|delline|editline|glow|billboard|size>", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be select — Select entity you're looking at", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be movehere — Move selected entity to you", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be remove — Remove selected entity", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be info — View selected entity details", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be list [type] — List all entities", prefix = false))
        sender.sendMessage(FormatManager.formatMessage(" /be save / reload — Persistence", prefix = false))
    }
}
