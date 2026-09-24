package gg.bitesize.plugin.managers

import com.destroystokyo.paper.profile.ProfileProperty
import gg.bitesize.plugin.models.*
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.*
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object EntityManager {

    private val entities = ConcurrentHashMap<UUID, BiteEntity>()
    private val bukkitEntities = ConcurrentHashMap<UUID, Entity>()
    private val selections = ConcurrentHashMap<UUID, UUID>() // player UUID → entity UUID

    // Companion entity tracking (name TextDisplays + Interaction hitboxes)
    data class CompanionSet(
        var nameDisplay: Entity? = null,
        var interaction: Entity? = null
    )
    private val companions = ConcurrentHashMap<UUID, CompanionSet>()

    // ── Selection ──────────────────────────────────────────────────

    fun select(playerUUID: UUID, entityUUID: UUID) {
        selections[playerUUID] = entityUUID
    }

    fun getSelection(playerUUID: UUID): BiteEntity? {
        val entityId = selections[playerUUID] ?: return null
        return entities[entityId]
    }

    fun clearSelection(playerUUID: UUID) {
        selections.remove(playerUUID)
    }

    // ── Core CRUD ──────────────────────────────────────────────────

    fun spawn(entity: BiteEntity): Entity? {
        val bukkit = when (entity) {
            is BiteNpc -> spawnNpc(entity)
            is BiteBlock -> spawnBlockDisplay(entity)
            is BiteItem -> spawnItemDisplay(entity)
            is BiteText -> spawnTextDisplay(entity)
            else -> return null
        } ?: return null

        entities[entity.id] = entity
        bukkitEntities[entity.id] = bukkit
        spawnCompanions(entity)
        return bukkit
    }

    fun despawn(id: UUID): Boolean {
        despawnCompanions(id)
        cleanupGlowTeam(id)
        val bukkit = bukkitEntities.remove(id) ?: return false
        entities.remove(id)
        bukkit.remove()
        selections.entries.removeIf { it.value == id }
        return true
    }

    fun get(id: UUID): BiteEntity? = entities[id]

    fun getBukkitEntity(id: UUID): Entity? = bukkitEntities[id]

    fun getAll(): Collection<BiteEntity> = entities.values

    fun teleport(id: UUID, location: Location): Boolean {
        val entity = entities[id] ?: return false
        val bukkit = bukkitEntities[id] ?: return false

        when (entity) {
            is BiteNpc -> entity.location = location
            is BiteBlock -> entity.location = location
            is BiteItem -> entity.location = location
            is BiteText -> entity.location = location
        }

        bukkit.teleport(location)

        // Teleport companion entities
        val scale = when (entity) {
            is BiteItem -> entity.scale
            is BiteBlock -> entity.scale
            else -> null
        }
        companions[id]?.let { set ->
            set.nameDisplay?.teleport(if (scale != null) calculateNamePosition(location, scale) else location)
            set.interaction?.teleport(location)
        }

        return true
    }

    fun despawnAll() {
        companions.values.forEach { set ->
            set.nameDisplay?.remove()
            set.interaction?.remove()
        }
        companions.clear()
        bukkitEntities.values.forEach { it.remove() }
        bukkitEntities.clear()
        entities.clear()
        selections.clear()
    }

    fun removeBukkitEntity(id: UUID) {
        bukkitEntities.remove(id)
        companions.remove(id)
    }

    fun respawn(entity: BiteEntity): Entity? {
        val bukkit = when (entity) {
            is BiteNpc -> spawnNpc(entity)
            is BiteBlock -> spawnBlockDisplay(entity)
            is BiteItem -> spawnItemDisplay(entity)
            is BiteText -> spawnTextDisplay(entity)
            else -> return null
        } ?: return null

        bukkitEntities[entity.id] = bukkit
        spawnCompanions(entity)
        return bukkit
    }

    fun updateEntity(entity: BiteEntity): Boolean {
        val result = when (entity) {
            is BiteNpc -> updateNpc(entity)
            is BiteBlock -> updateBlock(entity)
            is BiteItem -> updateItem(entity)
            is BiteText -> updateText(entity)
            else -> false
        }

        // Rebuild companion entities for items/blocks
        if (entity is BiteItem || entity is BiteBlock) {
            despawnCompanions(entity.id)
            spawnCompanions(entity)
        }

        return result
    }

    fun findByBukkitId(bukkitUUID: UUID): BiteEntity? {
        return entities.values.firstOrNull { entity ->
            bukkitEntities[entity.id]?.uniqueId == bukkitUUID
        }
    }

    /**
     * Finds a BiteEntity whose companion Interaction entity matches the given Bukkit UUID.
     */
    fun findByCompanionBukkitId(bukkitUUID: UUID): BiteEntity? {
        return companions.entries.firstOrNull { (_, set) ->
            set.interaction?.uniqueId == bukkitUUID
        }?.let { entities[it.key] }
    }

    fun getCompanions(id: UUID): CompanionSet? = companions[id]

    // ── Spawn helpers ──────────────────────────────────────────────

    private fun spawnNpc(npc: BiteNpc): Entity? {
        val world = npc.location.world ?: return null
        return world.spawn(npc.location, Mannequin::class.java) { mannequin ->
            mannequin.setGravity(false)
            mannequin.isInvulnerable = true
            mannequin.isPersistent = false
            mannequin.setImmovable(true)

            // Name (MiniMessage)
            npc.name?.let {
                mannequin.customName(FormatManager.parseMiniMessage(it))
                mannequin.isCustomNameVisible = true
            }

            // Description (MiniMessage)
            npc.description?.let {
                mannequin.setDescription(FormatManager.parseMiniMessage(it))
            }

            // Skin
            if (npc.skinValue != null) {
                val profile = ResolvableProfile.resolvableProfile()
                    .addProperty(ProfileProperty("textures", npc.skinValue!!, npc.skinSignature))
                    .build()
                mannequin.setProfile(profile)
            }

            // Equipment
            npc.itemInHand?.let { mannequin.equipment.setItemInMainHand(it) }
            npc.offHand?.let { mannequin.equipment.setItemInOffHand(it) }
            npc.helmet?.let { mannequin.equipment.setHelmet(it) }
            npc.chestplate?.let { mannequin.equipment.setChestplate(it) }
            npc.leggings?.let { mannequin.equipment.setLeggings(it) }
            npc.boots?.let { mannequin.equipment.setBoots(it) }

            // Glow (scoreboard team for color)
            if (npc.glowColor != null) {
                mannequin.isGlowing = true
            }
        }.also { mannequin ->
            // Apply glow team after spawn (needs the entity to exist)
            if (npc.glowColor != null) {
                applyGlowTeam(npc.id, mannequin, npc.glowColor!!)
            }
        }
    }

    private fun spawnBlockDisplay(block: BiteBlock): Entity? {
        val world = block.location.world ?: return null
        return world.spawn(block.location, BlockDisplay::class.java) { display ->
            display.block = block.itemStack.type.createBlockData()
            display.isPersistent = false
            applyTransform(display, block.scale, block.yaw, block.pitch)
            applyDisplayGlow(display, block.glowing, block.glowColor)
        }
    }

    private fun spawnItemDisplay(item: BiteItem): Entity? {
        val world = item.location.world ?: return null
        return world.spawn(item.location, ItemDisplay::class.java) { display ->
            display.setItemStack(item.itemStack)
            display.isPersistent = false
            applyTransform(display, item.scale, item.yaw, item.pitch)
            applyDisplayGlow(display, item.glowing, item.glowColor)

            // Bouncing items always face the player
            if (item.bouncing) {
                display.billboard = Display.Billboard.VERTICAL
            }
        }
    }

    private fun spawnTextDisplay(text: BiteText): Entity? {
        val world = text.location.world ?: return null
        return world.spawn(text.location, TextDisplay::class.java) { display ->
            display.isPersistent = false
            display.isSeeThrough = false

            // Billboard
            display.billboard = if (text.billboard) Display.Billboard.CENTER else Display.Billboard.FIXED

            // Render lines as MiniMessage
            if (text.lines.isNotEmpty()) {
                val content = text.lines.joinToString("\n")
                display.text(FormatManager.parseMiniMessage(content))
            }

            // Background color
            text.backgroundColor?.let {
                FormatManager.parseHexColor(it)?.let { color ->
                    display.backgroundColor = color
                }
            } ?: run {
                display.backgroundColor = Color.fromARGB(100, 0, 0, 0)
            }

            // Glow
            applyDisplayGlow(display, text.glowColor != null, text.glowColor)

            applyTransform(display, text.scale, 0f, 0f)
        }
    }

    // ── Companion entity helpers ───────────────────────────────────

    private fun spawnCompanions(entity: BiteEntity) {
        val nameText = when (entity) {
            is BiteItem -> entity.nameText
            is BiteBlock -> entity.nameText
            else -> return
        }
        val command = when (entity) {
            is BiteItem -> entity.command
            is BiteBlock -> entity.command
            else -> null
        }
        val scale = when (entity) {
            is BiteItem -> entity.scale
            is BiteBlock -> entity.scale
            else -> return
        }

        val set = CompanionSet()

        if (nameText != null) {
            set.nameDisplay = spawnNameCompanion(entity.location, nameText, scale)
        }
        if (command != null) {
            set.interaction = spawnInteractionCompanion(entity.location, scale)
        }

        if (set.nameDisplay != null || set.interaction != null) {
            companions[entity.id] = set
        }
    }

    private fun despawnCompanions(id: UUID) {
        companions.remove(id)?.let { set ->
            set.nameDisplay?.remove()
            set.interaction?.remove()
        }
    }

    private fun spawnNameCompanion(location: Location, nameText: String, scale: Vector3f): Entity? {
        val world = location.world ?: return null
        val nameLoc = calculateNamePosition(location, scale)
        return world.spawn(nameLoc, TextDisplay::class.java) { display ->
            display.text(FormatManager.parseMiniMessage(nameText))
            display.billboard = Display.Billboard.CENTER
            display.isPersistent = false
            display.backgroundColor = Color.fromARGB(0, 0, 0, 0) // Transparent
        }
    }

    private fun spawnInteractionCompanion(location: Location, scale: Vector3f): Entity? {
        val world = location.world ?: return null
        return world.spawn(location, Interaction::class.java) { interaction ->
            interaction.setInteractionWidth(scale.x.coerceAtLeast(1f))
            interaction.setInteractionHeight(scale.y.coerceAtLeast(1f))
            interaction.isPersistent = false
            interaction.isResponsive = true
        }
    }

    private fun calculateNamePosition(location: Location, scale: Vector3f): Location {
        return location.clone().add(0.0, (scale.y * 0.5 + 0.5).toDouble(), 0.0)
    }

    // ── Update helpers ─────────────────────────────────────────────

    private fun updateNpc(npc: BiteNpc): Boolean {
        val mannequin = bukkitEntities[npc.id] as? Mannequin ?: return false

        // Name
        if (npc.name != null) {
            mannequin.customName(FormatManager.parseMiniMessage(npc.name!!))
            mannequin.isCustomNameVisible = true
        } else {
            mannequin.customName(null)
            mannequin.isCustomNameVisible = false
        }

        // Description
        if (npc.description != null) {
            mannequin.setDescription(FormatManager.parseMiniMessage(npc.description!!))
        } else {
            mannequin.setDescription(null)
        }

        // Skin
        if (npc.skinValue != null) {
            val profile = ResolvableProfile.resolvableProfile()
                .addProperty(ProfileProperty("textures", npc.skinValue!!, npc.skinSignature))
                .build()
            mannequin.setProfile(profile)
        }

        // Equipment
        mannequin.equipment.setItemInMainHand(npc.itemInHand)
        mannequin.equipment.setItemInOffHand(npc.offHand)
        mannequin.equipment.setHelmet(npc.helmet)
        mannequin.equipment.setChestplate(npc.chestplate)
        mannequin.equipment.setLeggings(npc.leggings)
        mannequin.equipment.setBoots(npc.boots)

        // Glow
        if (npc.glowColor != null) {
            mannequin.isGlowing = true
            applyGlowTeam(npc.id, mannequin, npc.glowColor!!)
        } else {
            mannequin.isGlowing = false
            cleanupGlowTeam(npc.id)
        }

        return true
    }

    private fun updateBlock(block: BiteBlock): Boolean {
        val display = bukkitEntities[block.id] as? BlockDisplay ?: return false
        display.block = block.itemStack.type.createBlockData()
        applyTransform(display, block.scale, block.yaw, block.pitch)
        applyDisplayGlow(display, block.glowing, block.glowColor)
        return true
    }

    private fun updateItem(item: BiteItem): Boolean {
        val display = bukkitEntities[item.id] as? ItemDisplay ?: return false
        display.setItemStack(item.itemStack)
        applyTransform(display, item.scale, item.yaw, item.pitch)
        applyDisplayGlow(display, item.glowing, item.glowColor)

        // Billboard for bouncing
        display.billboard = if (item.bouncing) Display.Billboard.VERTICAL else Display.Billboard.FIXED

        return true
    }

    private fun updateText(text: BiteText): Boolean {
        val display = bukkitEntities[text.id] as? TextDisplay ?: return false

        // Billboard
        display.billboard = if (text.billboard) Display.Billboard.CENTER else Display.Billboard.FIXED

        // Text content
        if (text.lines.isNotEmpty()) {
            val content = text.lines.joinToString("\n")
            display.text(FormatManager.parseMiniMessage(content))
        } else {
            display.text(Component.empty())
        }

        // Background
        text.backgroundColor?.let {
            FormatManager.parseHexColor(it)?.let { color ->
                display.backgroundColor = color
            }
        }

        // Glow
        applyDisplayGlow(display, text.glowColor != null, text.glowColor)

        applyTransform(display, text.scale, 0f, 0f)
        return true
    }

    // ── Glow helpers ───────────────────────────────────────────────

    /**
     * Apply glow to Display entities using setGlowColorOverride (supports full RGB).
     */
    private fun applyDisplayGlow(display: Display, glowing: Boolean, glowColor: String?) {
        display.isGlowing = glowing
        if (glowing && glowColor != null) {
            FormatManager.parseColor(glowColor)?.let { display.setGlowColorOverride(it) }
        } else {
            display.setGlowColorOverride(null)
        }
    }

    /**
     * Apply glow to non-Display entities (Mannequin) using scoreboard teams.
     * Only supports the 16 named Minecraft colors (no hex).
     */
    private fun applyGlowTeam(biteId: UUID, entity: Entity, colorName: String) {
        val teamName = "be_${biteId.toString().substring(0, 8)}"
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
        FormatManager.namedTextColorFromString(colorName)?.let { team.color(it) }
        team.addEntity(entity)
    }

    private fun cleanupGlowTeam(biteId: UUID) {
        val teamName = "be_${biteId.toString().substring(0, 8)}"
        Bukkit.getScoreboardManager().mainScoreboard.getTeam(teamName)?.unregister()
    }

    // ── Transform helpers ──────────────────────────────────────────

    private fun applyTransform(display: Display, scale: Vector3f, yaw: Float, pitch: Float) {
        val t = display.transformation
        display.transformation = Transformation(
            t.translation,
            Quaternionf()
                .rotateY(Math.toRadians(yaw.toDouble()).toFloat())
                .rotateX(Math.toRadians(pitch.toDouble()).toFloat()),
            scale,
            t.rightRotation
        )
    }
}