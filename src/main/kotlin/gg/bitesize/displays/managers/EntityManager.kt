package gg.bitesize.displays.managers

import gg.bitesize.displays.models.*
import gg.bitesize.displays.platform.Platform
import gg.bitesize.displays.platform.Skin
import gg.bitesize.displays.util.Colors
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

    private val platform get() = Platform.current

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
        val bukkit = spawnBukkit(entity) ?: return null

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
        val scale = (entity as? BiteDisplay)?.scale
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
        val bukkit = spawnBukkit(entity) ?: return null

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
        if (entity is BiteDisplay) {
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

    private fun spawnBukkit(entity: BiteEntity): Entity? = when (entity) {
        is BiteNpc -> spawnNpc(entity)
        is BiteBlock -> spawnBlockDisplay(entity)
        is BiteItem -> spawnItemDisplay(entity)
        is BiteText -> spawnTextDisplay(entity)
        else -> null
    }

    private fun spawnNpc(npc: BiteNpc): Entity? {
        val world = npc.location.world ?: return null
        return world.spawn(npc.location, Mannequin::class.java) { mannequin ->
            mannequin.setGravity(false)
            mannequin.isInvulnerable = true
            mannequin.isPersistent = false
            mannequin.setImmovable(true)

            // Name (MiniMessage)
            npc.name?.let {
                platform.setName(mannequin, it)
                mannequin.isCustomNameVisible = true
            }

            // Description (MiniMessage)
            npc.description?.let {
                platform.setDescription(mannequin, it)
            }

            // Skin
            npc.skin()?.let { platform.setSkin(mannequin, it) }

            // Equipment
            mannequin.equipment?.apply {
                npc.itemInHand?.let { setItemInMainHand(it) }
                npc.offHand?.let { setItemInOffHand(it) }
                npc.helmet?.let { helmet = it }
                npc.chestplate?.let { chestplate = it }
                npc.leggings?.let { leggings = it }
                npc.boots?.let { boots = it }
            }

            // Glow (scoreboard team for color)
            if (npc.glowColor != null) {
                mannequin.isGlowing = true
            }
        }.also { mannequin ->
            // Apply glow team after spawn (needs the entity to exist)
            npc.glowColor?.let { applyGlowTeam(npc.id, mannequin, it) }
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
                platform.setText(display, text.lines.joinToString("\n"))
            }

            display.backgroundColor = backgroundOf(text)

            // Glow
            applyDisplayGlow(display, text.glowColor != null, text.glowColor)

            applyTransform(display, text.scale, 0f, 0f)
        }
    }

    // ── Companion entity helpers ───────────────────────────────────

    private fun spawnCompanions(entity: BiteEntity) {
        if (entity !is BiteDisplay) return

        val set = CompanionSet()

        entity.nameText?.let {
            set.nameDisplay = spawnNameCompanion(entity.location, it, entity.scale)
        }
        if (entity.command != null) {
            set.interaction = spawnInteractionCompanion(entity.location, entity.scale)
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
            platform.setText(display, nameText)
            display.billboard = Display.Billboard.CENTER
            display.isPersistent = false
            display.backgroundColor = Color.fromARGB(0, 0, 0, 0) // Transparent
        }
    }

    private fun spawnInteractionCompanion(location: Location, scale: Vector3f): Entity? {
        val world = location.world ?: return null
        return world.spawn(location, Interaction::class.java) { interaction ->
            interaction.interactionWidth = scale.x.coerceAtLeast(1f)
            interaction.interactionHeight = scale.y.coerceAtLeast(1f)
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
        platform.setName(mannequin, npc.name)
        mannequin.isCustomNameVisible = npc.name != null

        // Description
        platform.setDescription(mannequin, npc.description)

        // Skin
        npc.skin()?.let { platform.setSkin(mannequin, it) }

        // Equipment
        mannequin.equipment?.apply {
            setItemInMainHand(npc.itemInHand)
            setItemInOffHand(npc.offHand)
            helmet = npc.helmet
            chestplate = npc.chestplate
            leggings = npc.leggings
            boots = npc.boots
        }

        // Glow
        val glowColor = npc.glowColor
        if (glowColor != null) {
            mannequin.isGlowing = true
            applyGlowTeam(npc.id, mannequin, glowColor)
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
        platform.setText(display, text.lines.joinToString("\n"))

        display.backgroundColor = backgroundOf(text)

        // Glow
        applyDisplayGlow(display, text.glowColor != null, text.glowColor)

        applyTransform(display, text.scale, 0f, 0f)
        return true
    }

    private fun BiteNpc.skin(): Skin? = skinValue?.let { Skin(it, skinSignature) }

    private fun backgroundOf(text: BiteText): Color {
        return text.backgroundColor?.let { Colors.parseHex(it) } ?: ConfigManager.settings.defaultTextBackground
    }

    // ── Glow helpers ───────────────────────────────────────────────

    /**
     * Apply glow to Display entities using setGlowColorOverride (supports full RGB).
     */
    private fun applyDisplayGlow(display: Display, glowing: Boolean, glowColor: String?) {
        display.isGlowing = glowing
        if (glowing && glowColor != null) {
            Colors.parse(glowColor)?.let { display.glowColorOverride = it }
        } else {
            display.glowColorOverride = null
        }
    }

    /**
     * Apply glow to non-Display entities (Mannequin) using scoreboard teams.
     * Only supports the 16 named Minecraft colors (no hex).
     */
    private fun applyGlowTeam(biteId: UUID, entity: Entity, colorName: String) {
        val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard ?: return
        val teamName = glowTeamName(biteId)
        val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)
        Colors.teamColor(colorName)?.let { team.color = it }
        // Non-player entities join teams by UUID
        team.addEntry(entity.uniqueId.toString())
    }

    private fun cleanupGlowTeam(biteId: UUID) {
        Bukkit.getScoreboardManager()?.mainScoreboard?.getTeam(glowTeamName(biteId))?.unregister()
    }

    private fun glowTeamName(biteId: UUID) = "bd_${biteId.toString().take(8)}"

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
