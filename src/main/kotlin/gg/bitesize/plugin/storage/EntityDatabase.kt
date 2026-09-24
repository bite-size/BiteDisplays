package gg.bitesize.plugin.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import gg.bitesize.plugin.managers.EntityManager
import gg.bitesize.plugin.models.*
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
 * Flat-file JSON persistence for all tracked BiteEntities.
 * Saves to plugins/BiteEntities/entities.json.
 */
object EntityDatabase {

    private lateinit var file: File
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun initialize(dataFolder: File) {
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

        for (data in list) {
            val entity = fromSerialized(data) ?: continue
            EntityManager.spawn(entity)
        }
    }

    // ── Serialization ──────────────────────────────────────────────

    private fun toSerialized(entity: BiteEntity): SerializedEntity? {
        val loc = entity.location
        val base = SerializedEntity(
            type = when (entity) {
                is BiteNpc -> "npc"
                is BiteBlock -> "block"
                is BiteItem -> "item"
                is BiteText -> "text"
                else -> return null
            },
            id = entity.id.toString(),
            world = loc.world?.name ?: return null,
            x = loc.x, y = loc.y, z = loc.z,
            yaw = loc.yaw, pitch = loc.pitch
        )

        return when (entity) {
            is BiteNpc -> base.copy(
                name = entity.name,
                skinValue = entity.skinValue,
                skinSignature = entity.skinSignature,
                command = entity.command,
                glowColor = entity.glowColor,
                description = entity.description,
                sound = entity.sound,
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
                displayCommand = entity.command,
                displayGlowing = entity.glowing,
                displayGlowColor = entity.glowColor
            )
            is BiteItem -> base.copy(
                itemStack = encodeItem(entity.itemStack),
                scaleX = entity.scale.x, scaleY = entity.scale.y, scaleZ = entity.scale.z,
                rotationYaw = entity.yaw, rotationPitch = entity.pitch,
                nameText = entity.nameText,
                displayCommand = entity.command,
                displayGlowing = entity.glowing,
                displayGlowColor = entity.glowColor,
                bouncing = entity.bouncing
            )
            is BiteText -> base.copy(
                lines = entity.lines,
                backgroundColor = entity.backgroundColor,
                scaleX = entity.scale.x, scaleY = entity.scale.y, scaleZ = entity.scale.z,
                textGlowColor = entity.glowColor,
                billboard = entity.billboard
            )
            else -> null
        }
    }

    private fun fromSerialized(data: SerializedEntity): BiteEntity? {
        val world = Bukkit.getWorld(data.world) ?: return null
        val location = Location(world, data.x, data.y, data.z, data.yaw, data.pitch)
        val id = UUID.fromString(data.id)

        return when (data.type) {
            "npc" -> BiteNpc(
                id = id,
                location = location,
                name = data.name,
                skinValue = data.skinValue,
                skinSignature = data.skinSignature,
                command = data.command,
                glowColor = data.glowColor,
                description = data.description,
                sound = data.sound,
                itemInHand = data.itemInHand?.let { decodeItem(it) },
                offHand = data.offHand?.let { decodeItem(it) },
                helmet = data.helmet?.let { decodeItem(it) },
                chestplate = data.chestplate?.let { decodeItem(it) },
                leggings = data.leggings?.let { decodeItem(it) },
                boots = data.boots?.let { decodeItem(it) }
            )
            "block" -> BiteBlock(
                id = id,
                location = location,
                itemStack = data.itemStack?.let { decodeItem(it) } ?: return null,
                scale = Vector3f(data.scaleX, data.scaleY, data.scaleZ),
                yaw = data.rotationYaw,
                pitch = data.rotationPitch,
                nameText = data.nameText,
                command = data.displayCommand,
                glowing = data.displayGlowing,
                glowColor = data.displayGlowColor
            )
            "item" -> BiteItem(
                id = id,
                location = location,
                itemStack = data.itemStack?.let { decodeItem(it) } ?: return null,
                scale = Vector3f(data.scaleX, data.scaleY, data.scaleZ),
                yaw = data.rotationYaw,
                pitch = data.rotationPitch,
                nameText = data.nameText,
                command = data.displayCommand,
                glowing = data.displayGlowing,
                glowColor = data.displayGlowColor,
                bouncing = data.bouncing
            )
            "text" -> BiteText(
                id = id,
                location = location,
                lines = data.lines?.toMutableList() ?: mutableListOf(),
                scale = Vector3f(data.scaleX, data.scaleY, data.scaleZ),
                backgroundColor = data.backgroundColor,
                glowColor = data.textGlowColor,
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
    val displayCommand: String? = null,
    val displayGlowing: Boolean = false,
    val displayGlowColor: String? = null,
    val bouncing: Boolean = false,
    // ── Text display fields ──
    val lines: List<String>? = null,
    val backgroundColor: String? = null,
    val textGlowColor: String? = null,
    val billboard: Boolean = false
)
