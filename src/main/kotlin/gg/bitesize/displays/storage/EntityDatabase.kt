package gg.bitesize.displays.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import gg.bitesize.displays.BiteDisplays.Companion.INSTANCE
import gg.bitesize.displays.managers.EntityManager
import gg.bitesize.displays.models.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.joml.Vector3f
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.UUID

/**
 * Flat-file JSON persistence for all tracked BiteDisplays.
 * Saves to plugins/BiteDisplays/entities.json.
 */
object EntityDatabase {

    private lateinit var file: File
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun initialize() {
        val dataFolder = INSTANCE.dataFolder
        if (!dataFolder.exists()) dataFolder.mkdirs()
        file = File(dataFolder, "entities.json")
        if (!file.exists()) file.createNewFile()
    }

    fun save() {
        val serialized = EntityManager.getAll().mapNotNull { toSerialized(it) }
        file.writeText(gson.toJson(serialized))
    }

    fun load() {
        val text = file.readText()
        if (text.isBlank()) return

        val type = object : TypeToken<List<SerializedEntity>>() {}.type
        val list: List<SerializedEntity> = gson.fromJson(text, type) ?: return

        // Entities saved before IDs were numbered (number 0) get the next free number of their type
        val highest = list.groupBy { it.type }.mapValues { (_, entries) -> entries.maxOf { it.number } }.toMutableMap()

        for (data in list) {
            val number = data.number.takeIf { it > 0 }
                ?: (highest.getValue(data.type) + 1).also { highest[data.type] = it }
            val entity = fromSerialized(data, number) ?: continue
            EntityManager.spawn(entity)
        }
    }

    // ── Serialization ──────────────────────────────────────────────

    private fun toSerialized(entity: BiteEntity): SerializedEntity? {
        val loc = entity.location
        val base = SerializedEntity(
            type = entity.type.key,
            id = entity.id.toString(),
            number = entity.number,
            world = loc.world?.name ?: return null,
            x = loc.x, y = loc.y, z = loc.z,
            yaw = loc.yaw, pitch = loc.pitch
        )

        return when (entity) {
            is BiteNpc -> base.copy(
                name = entity.name,
                skinValue = entity.skinValue,
                skinSignature = entity.skinSignature,
                command = entity.command?.command,
                commandRunner = entity.command?.runner?.key,
                glowColor = entity.glowColor,
                description = entity.description,
                sound = entity.sound,
                npcScale = entity.scale,
                itemInHand = entity.itemInHand?.let { encodeItem(it) },
                offHand = entity.offHand?.let { encodeItem(it) },
                helmet = entity.helmet?.let { encodeItem(it) },
                chestplate = entity.chestplate?.let { encodeItem(it) },
                leggings = entity.leggings?.let { encodeItem(it) },
                boots = entity.boots?.let { encodeItem(it) }
            )
            is BiteBlock -> base.copy(
                itemStack = encodeItem(entity.itemStack),
                scaleX = entity.scale.x, scaleY = entity.scale.y, scaleZ = entity.scale.z,
                rotationYaw = entity.yaw, rotationPitch = entity.pitch,
                nameText = entity.nameText,
                displayCommand = entity.command?.command,
                commandRunner = entity.command?.runner?.key,
                displayGlowColor = entity.glowColor
            )
            is BiteItem -> base.copy(
                itemStack = encodeItem(entity.itemStack),
                scaleX = entity.scale.x, scaleY = entity.scale.y, scaleZ = entity.scale.z,
                rotationYaw = entity.yaw, rotationPitch = entity.pitch,
                nameText = entity.nameText,
                displayCommand = entity.command?.command,
                commandRunner = entity.command?.runner?.key,
                displayGlowColor = entity.glowColor,
                bouncing = entity.bouncing
            )
            is BiteText -> base.copy(
                lines = entity.lines,
                backgroundColor = entity.backgroundColor,
                scaleX = entity.scale.x, scaleY = entity.scale.y, scaleZ = entity.scale.z,
                displayCommand = entity.command?.command,
                commandRunner = entity.command?.runner?.key,
                shadow = entity.shadow,
                billboard = entity.billboard
            )
            else -> null
        }
    }

    private fun fromSerialized(data: SerializedEntity, number: Int): BiteEntity? {
        val world = Bukkit.getWorld(data.world) ?: return null
        val location = Location(world, data.x, data.y, data.z, data.yaw, data.pitch)
        val id = UUID.fromString(data.id)

        // Commands saved before they had a runner were run by the console
        val runner = CommandRunner.fromKey(data.commandRunner) ?: CommandRunner.CONSOLE
        fun command(command: String?) = command?.let { ClickCommand(runner, it) }

        // Displays used to glow with a separate flag, and without a color they glowed white
        val displayGlow = data.displayGlowColor ?: "white".takeIf { data.displayGlowing == true }

        return when (data.type) {
            "npc" -> BiteNpc(
                id = id,
                number = number,
                location = location,
                name = data.name,
                skinValue = data.skinValue,
                skinSignature = data.skinSignature,
                command = command(data.command),
                glowColor = data.glowColor,
                description = data.description,
                sound = data.sound,
                // Gson skips constructors, so a missing field reads as 0 rather than the default
                scale = data.npcScale.takeIf { it > 0 } ?: 1.0,
                itemInHand = data.itemInHand?.let { decodeItem(it) },
                offHand = data.offHand?.let { decodeItem(it) },
                helmet = data.helmet?.let { decodeItem(it) },
                chestplate = data.chestplate?.let { decodeItem(it) },
                leggings = data.leggings?.let { decodeItem(it) },
                boots = data.boots?.let { decodeItem(it) }
            )
            "block" -> BiteBlock(
                id = id,
                number = number,
                location = location,
                itemStack = data.itemStack?.let { decodeItem(it) } ?: return null,
                scale = Vector3f(data.scaleX, data.scaleY, data.scaleZ),
                yaw = data.rotationYaw,
                pitch = data.rotationPitch,
                nameText = data.nameText,
                command = command(data.displayCommand),
                glowColor = displayGlow
            )
            "item" -> BiteItem(
                id = id,
                number = number,
                location = location,
                itemStack = data.itemStack?.let { decodeItem(it) } ?: return null,
                scale = Vector3f(data.scaleX, data.scaleY, data.scaleZ),
                yaw = data.rotationYaw,
                pitch = data.rotationPitch,
                nameText = data.nameText,
                command = command(data.displayCommand),
                glowColor = displayGlow,
                bouncing = data.bouncing
            )
            "text" -> BiteText(
                id = id,
                number = number,
                location = location,
                lines = data.lines?.toMutableList() ?: mutableListOf(),
                scale = Vector3f(data.scaleX, data.scaleY, data.scaleZ),
                backgroundColor = data.backgroundColor,
                command = command(data.displayCommand),
                shadow = data.shadow,
                billboard = data.billboard
            )
            else -> null
        }
    }

    // ── ItemStack base64 encoding ──────────────────────────────────

    private fun encodeItem(item: ItemStack): String {
        val stream = ByteArrayOutputStream()
        BukkitObjectOutputStream(stream).use { it.writeObject(item) }
        return Base64.getEncoder().encodeToString(stream.toByteArray())
    }

    private fun decodeItem(data: String): ItemStack? {
        return try {
            val bytes = Base64.getDecoder().decode(data)
            BukkitObjectInputStream(ByteArrayInputStream(bytes)).use {
                it.readObject() as ItemStack
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Flat DTO that unions all fields across entity types.
 * The [type] discriminator determines which fields are relevant.
 */
private data class SerializedEntity(
    val type: String,
    val id: String,
    /** The number in the ID players see (npc-1). 0 in files saved before IDs were numbered. */
    val number: Int = 0,
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    // ── NPC fields ──
    val name: String? = null,
    val skinValue: String? = null,
    val skinSignature: String? = null,
    val command: String? = null,
    val glowColor: String? = null,
    val description: String? = null,
    val sound: String? = null,
    val npcScale: Double = 1.0,
    val itemInHand: String? = null,
    val offHand: String? = null,
    val helmet: String? = null,
    val chestplate: String? = null,
    val leggings: String? = null,
    val boots: String? = null,
    // ── Display shared fields ──
    val itemStack: String? = null,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val rotationYaw: Float = 0f,
    val rotationPitch: Float = 0f,
    val nameText: String? = null,
    /** Item, block, and text click command. */
    val displayCommand: String? = null,
    /** Only read, from files saved before glow became a color alone. */
    val displayGlowing: Boolean? = null,
    val displayGlowColor: String? = null,
    val bouncing: Boolean = false,
    // ── Shared by NPCs and displays ──
    /** console or player, for [command] and [displayCommand]. */
    val commandRunner: String? = null,
    // ── Text display fields ──
    val lines: List<String>? = null,
    val backgroundColor: String? = null,
    val shadow: Boolean = false,
    val billboard: Boolean = false
)
