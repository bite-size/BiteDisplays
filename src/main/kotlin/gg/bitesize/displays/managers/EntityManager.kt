package gg.bitesize.displays.managers

import gg.bitesize.displays.models.*
import gg.bitesize.displays.platform.Platform
import gg.bitesize.displays.platform.Skin
import gg.bitesize.displays.util.Colors
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.util.BoundingBox
import org.bukkit.util.Transformation
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

object EntityManager {

    private val entities = ConcurrentHashMap<UUID, BiteEntity>()
    private val bukkitEntities = ConcurrentHashMap<UUID, Entity>()
    private val owners = ConcurrentHashMap<UUID, UUID>() // Bukkit entity UUID (main or companion) → entity UUID
    private val selections = ConcurrentHashMap<UUID, UUID>() // player UUID → entity UUID

    // Companion entity tracking (name TextDisplays + Interaction hitboxes)
    data class CompanionSet(
        var nameDisplay: Entity? = null,
        var interaction: Entity? = null
    ) {
        fun all(): List<Entity> = listOfNotNull(nameDisplay, interaction)
    }
    private val companions = ConcurrentHashMap<UUID, CompanionSet>()

    private val platform get() = Platform.current

    // Text displays render 40 pixels to a block. A default-font character is about 6 pixels wide and a line 10 tall.
    private const val TEXT_PIXEL = 0.025f
    private const val TEXT_CHAR_WIDTH = 6
    private const val TEXT_LINE_HEIGHT = 10
    private const val TEXT_LINE_WIDTH = 200 // vanilla's default wrap width

    private const val MIN_HITBOX = 0.5f
    private const val NAME_GAP = 0.1
    private const val SIGHT_MARGIN = 0.15

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

    /**
     * The entity in the player's line of sight. Display entities have no hitbox, so the ray is tested
     * against [boundsOf]. Entities the player stands inside only count when nothing else is hit.
     */
    fun findInSight(eye: Location, range: Double): BiteEntity? {
        val start = eye.toVector()
        val direction = eye.direction

        return entities.values.asSequence()
            .filter { it.location.world?.uid == eye.world?.uid && bukkitEntities.containsKey(it.id) }
            .mapNotNull { entity ->
                val box = boundsOf(entity, eye)?.expand(SIGHT_MARGIN) ?: return@mapNotNull null
                val hit = box.rayTrace(start, direction, range) ?: return@mapNotNull null
                Triple(entity, box.contains(start), start.distanceSquared(hit.hitPosition))
            }
            .minWithOrNull(compareBy({ it.second }, { it.third }))
            ?.first
    }

    // ── Core CRUD ──────────────────────────────────────────────────

    fun spawn(entity: BiteEntity): Entity? {
        val bukkit = spawnBukkit(entity) ?: return null

        entities[entity.id] = entity
        track(entity, bukkit)
        spawnCompanions(entity)
        return bukkit
    }

    /** Removes the entity for good, whether or not its chunk is loaded. */
    fun despawn(id: UUID): Boolean {
        entities.remove(id) ?: return false
        despawnCompanions(id)
        cleanupGlowTeam(id)
        bukkitEntities.remove(id)?.let { bukkit ->
            owners.remove(bukkit.uniqueId)
            bukkit.remove()
        }
        selections.entries.removeIf { it.value == id }
        return true
    }

    fun get(id: UUID): BiteEntity? = entities[id]

    fun getBukkitEntity(id: UUID): Entity? = bukkitEntities[id]

    fun getAll(): Collection<BiteEntity> = entities.values

    /** The next number for a new entity of [type]: one past the highest in use. */
    fun nextNumber(type: BiteType): Int {
        return (entities.values.filter { it.type == type }.maxOfOrNull { it.number } ?: 0) + 1
    }

    /** Finds an entity by the ID players see, like npc-1. */
    fun findByDisplayId(displayId: String): BiteEntity? {
        return entities.values.firstOrNull { it.displayId.equals(displayId, ignoreCase = true) }
    }

    /**
     * Where a new or moved entity goes: exactly where the player stands, but level, so nothing tilts.
     * Blocks also turn to the nearest quarter so they line up with the world.
     */
    fun placement(type: BiteType, location: Location): Location {
        return location.clone().apply {
            pitch = 0f
            if (type == BiteType.BLOCK) yaw = Math.round(yaw / 90f) * 90f
        }
    }

    fun teleport(id: UUID, location: Location): Boolean {
        val entity = entities[id] ?: return false

        when (entity) {
            is BiteNpc -> entity.location = location
            is BiteBlock -> entity.location = location
            is BiteItem -> entity.location = location
            is BiteText -> entity.location = location
        }

        // Chunk loads and unloads find entities by their location, so one moved into an unloaded chunk
        // leaves the world now and spawns when that chunk loads
        val world = location.world
        if (world == null || !world.isChunkLoaded(location.blockX shr 4, location.blockZ shr 4)) {
            despawnCompanions(id)
            bukkitEntities.remove(id)?.let { bukkit ->
                owners.remove(bukkit.uniqueId)
                bukkit.remove()
            }
            return true
        }

        // An entity in an unloaded chunk (selected by ID) has no handle, so it's spawned at the new spot instead
        val bukkit = bukkitEntities[id]
        if (bukkit == null) {
            respawn(entity)
            return true
        }

        bukkit.teleport(location)
        rebuildCompanions(entity)
        return true
    }

    fun despawnAll() {
        companions.values.forEach { set -> set.all().forEach { it.remove() } }
        companions.clear()
        bukkitEntities.values.forEach { it.remove() }
        bukkitEntities.clear()
        owners.clear()
        entities.clear()
        selections.clear()
    }

    /** Drops the handles of an entity whose chunk unloaded. Its data stays for the next chunk load. */
    fun removeBukkitEntity(id: UUID) {
        bukkitEntities.remove(id)?.let { owners.remove(it.uniqueId) }
        companions.remove(id)?.all()?.forEach { owners.remove(it.uniqueId) }
    }

    fun respawn(entity: BiteEntity): Entity? {
        val bukkit = spawnBukkit(entity) ?: return null

        track(entity, bukkit)
        spawnCompanions(entity)
        return bukkit
    }

    fun updateEntity(entity: BiteEntity): Boolean {
        // Unloaded entities pick up the changes when their chunk loads
        val bukkit = bukkitEntities[entity.id] ?: return false

        when {
            entity is BiteNpc && bukkit is Mannequin -> {
                applyNpc(entity, bukkit)
                applyNpcGlow(entity, bukkit)
            }
            entity is BiteBlock && bukkit is BlockDisplay -> applyBlock(entity, bukkit)
            entity is BiteItem && bukkit is ItemDisplay -> applyItem(entity, bukkit)
            entity is BiteText && bukkit is TextDisplay -> applyText(entity, bukkit)
            else -> return false
        }

        // The hitbox and name label follow the name, command, size, and rotation
        rebuildCompanions(entity)
        return true
    }

    /** The entity a Bukkit entity belongs to, whether it's the entity itself or its hitbox or name label. */
    fun findByBukkitId(bukkitUUID: UUID): BiteEntity? {
        return owners[bukkitUUID]?.let { entities[it] }
    }

    private fun track(entity: BiteEntity, bukkit: Entity) {
        bukkitEntities.put(entity.id, bukkit)?.let { owners.remove(it.uniqueId) }
        owners[bukkit.uniqueId] = entity.id
    }

    // ── Spawn helpers ──────────────────────────────────────────────

    private fun spawnBukkit(entity: BiteEntity): Entity? = when (entity) {
        is BiteNpc -> spawnNpc(entity)
        is BiteBlock -> spawnDisplay(entity, BlockDisplay::class.java) { applyBlock(entity, it) }
        is BiteItem -> spawnDisplay(entity, ItemDisplay::class.java) { applyItem(entity, it) }
        is BiteText -> spawnDisplay(entity, TextDisplay::class.java) { applyText(entity, it) }
        else -> null
    }

    private fun spawnNpc(npc: BiteNpc): Entity? {
        val world = npc.location.world ?: return null
        return world.spawn(npc.location, Mannequin::class.java) { mannequin ->
            mannequin.setGravity(false)
            mannequin.isInvulnerable = true
            mannequin.isPersistent = false
            mannequin.setImmovable(true)
            applyNpc(npc, mannequin)
        }.also { mannequin ->
            // The glow team takes the entity's UUID, so it's applied once the entity exists
            applyNpcGlow(npc, mannequin)
        }
    }

    private fun <T : Display> spawnDisplay(entity: BiteEntity, type: Class<T>, apply: (T) -> Unit): Entity? {
        val world = entity.location.world ?: return null
        return world.spawn(entity.location, type) { display ->
            display.isPersistent = false
            apply(display)
        }
    }

    // ── Apply helpers (shared by spawn and update) ─────────────────

    private fun applyNpc(npc: BiteNpc, mannequin: Mannequin) {
        platform.setName(mannequin, npc.name?.let(::styled))
        mannequin.isCustomNameVisible = npc.name != null

        platform.setDescription(mannequin, npc.description?.let(::styled))

        npc.skin()?.let { platform.setSkin(mannequin, it) }

        mannequin.getAttribute(Attribute.SCALE)?.baseValue = npc.scale

        mannequin.equipment?.apply {
            setItemInMainHand(npc.itemInHand)
            setItemInOffHand(npc.offHand)
            helmet = npc.helmet
            chestplate = npc.chestplate
            leggings = npc.leggings
            boots = npc.boots
        }
    }

    private fun applyNpcGlow(npc: BiteNpc, mannequin: Mannequin) {
        val glowColor = npc.glowColor
        mannequin.isGlowing = glowColor != null
        if (glowColor != null) {
            applyGlowTeam(npc.id, mannequin, glowColor)
        } else {
            cleanupGlowTeam(npc.id)
        }
    }

    private fun applyBlock(block: BiteBlock, display: BlockDisplay) {
        display.block = block.itemStack.type.createBlockData()
        display.transformation = transformationOf(block)
        applyDisplayGlow(display, block.glowColor)
    }

    private fun applyItem(item: BiteItem, display: ItemDisplay) {
        display.setItemStack(item.itemStack)
        display.transformation = transformationOf(item)
        applyDisplayGlow(display, item.glowColor)

        // Bouncing items always face the player
        display.billboard = if (item.bouncing) Display.Billboard.VERTICAL else Display.Billboard.FIXED
    }

    private fun applyText(text: BiteText, display: TextDisplay) {
        display.isSeeThrough = false
        display.billboard = if (text.billboard) Display.Billboard.CENTER else Display.Billboard.FIXED
        platform.setText(display, text.lines.joinToString("\n") { styled(it) })
        display.backgroundColor = backgroundOf(text)
        display.isShadowed = text.shadow
        display.transformation = transformationOf(text)
    }

    private fun BiteNpc.skin(): Skin? = skinValue?.let { Skin(it, skinSignature) }

    private fun backgroundOf(text: BiteText): Color {
        return text.backgroundColor?.let { Colors.parseHex(it) } ?: ConfigManager.settings.defaultTextBackground
    }

    /** Platform renders MiniMessage, and players may also type legacy codes. */
    private fun styled(text: String): String = FormatManager.legacyToMiniMessage(text)

    // ── Companion entity helpers ───────────────────────────────────

    private fun spawnCompanions(entity: BiteEntity) {
        // Text is its own label, so it only needs a hitbox for its click command
        val (nameText, command) = when (entity) {
            is BiteDisplay -> entity.nameText to entity.command
            is BiteText -> null to entity.command
            else -> return
        }
        if (nameText == null && command == null) return
        val bounds = boundsOf(entity) ?: return

        val set = CompanionSet(
            nameDisplay = nameText?.let { spawnNameLabel(entity, it, bounds) },
            interaction = command?.let { spawnHitbox(entity, bounds) },
        )
        set.all().forEach { owners[it.uniqueId] = entity.id }
        companions[entity.id] = set
    }

    private fun despawnCompanions(id: UUID) {
        companions.remove(id)?.all()?.forEach { companion ->
            owners.remove(companion.uniqueId)
            companion.remove()
        }
    }

    /** Companions only exist next to a spawned entity, not in unloaded chunks. */
    private fun rebuildCompanions(entity: BiteEntity) {
        despawnCompanions(entity.id)
        if (bukkitEntities.containsKey(entity.id)) spawnCompanions(entity)
    }

    /** A name centered above the model, like a nametag. */
    private fun spawnNameLabel(display: BiteEntity, nameText: String, bounds: BoundingBox): Entity? {
        val world = display.location.world ?: return null
        val labelLocation = Location(world, bounds.centerX, bounds.maxY + NAME_GAP, bounds.centerZ)
        return world.spawn(labelLocation, TextDisplay::class.java) { label ->
            platform.setText(label, styled(nameText))
            label.billboard = Display.Billboard.CENTER
            label.isPersistent = false
            // The background players chose for nametags and chat
            label.isDefaultBackground = true
        }
    }

    /**
     * An Interaction hitbox covering the model, so clicks land where the display appears.
     * Text that faces players gets a footprint as wide as the text, so it covers it from every side.
     */
    private fun spawnHitbox(display: BiteEntity, bounds: BoundingBox): Entity? {
        val world = display.location.world ?: return null
        val base = Location(world, bounds.centerX, bounds.minY, bounds.centerZ)
        return world.spawn(base, Interaction::class.java) { interaction ->
            interaction.interactionWidth = max(bounds.widthX, bounds.widthZ).toFloat().coerceAtLeast(MIN_HITBOX)
            interaction.interactionHeight = bounds.height.toFloat().coerceAtLeast(MIN_HITBOX)
            interaction.isPersistent = false
            interaction.isResponsive = true
        }
    }

    // ── Bounds ─────────────────────────────────────────────────────

    /**
     * The space an entity appears to fill, in world coordinates. Display entities have no hitbox,
     * so this is what selection aims at, and where a display's hitbox and name label go.
     * Text and bouncing items turn to face whoever looks at them, as seen by [viewer] if given.
     */
    fun boundsOf(entity: BiteEntity, viewer: Location? = null): BoundingBox? = when (entity) {
        is BiteNpc -> bukkitEntities[entity.id]?.boundingBox
        // Block models span 0 to 1 before their transformation, item models -0.5 to 0.5
        is BiteBlock -> modelBounds(entity.location, transformationOf(entity), Vector3f(0f), Vector3f(1f), facesViewer = false, viewer)
        is BiteItem -> modelBounds(entity.location, transformationOf(entity), Vector3f(-0.5f), Vector3f(0.5f), facesViewer = entity.bouncing, viewer)
        is BiteText -> textBounds(entity, viewer)
        else -> null
    }

    /** Text grows up from its location, centered, and wraps at the line width. The size is an estimate. */
    private fun textBounds(text: BiteText, viewer: Location?): BoundingBox {
        val widths = text.lines.map { FormatManager.stripFormatting(it).length * TEXT_CHAR_WIDTH }
        val rows = widths.sumOf { max(1, ceil(it.toDouble() / TEXT_LINE_WIDTH).toInt()) }

        // Plus a pixel of background padding on each side
        val halfWidth = ((widths.maxOrNull() ?: 0).coerceAtMost(TEXT_LINE_WIDTH) + 2) * TEXT_PIXEL / 2
        val height = (rows * TEXT_LINE_HEIGHT + 2) * TEXT_PIXEL

        return modelBounds(text.location, transformationOf(text),
            Vector3f(-halfWidth, 0f, -TEXT_PIXEL), Vector3f(halfWidth, height, TEXT_PIXEL), facesViewer = text.billboard, viewer)
    }

    /**
     * The box around a model spanning [min] to [max], after its transformation and its rotation.
     * Without a [viewer], a model that faces the viewer could point any way, so its footprint covers every direction.
     */
    private fun modelBounds(location: Location, transformation: Transformation, min: Vector3f, max: Vector3f,
                            facesViewer: Boolean, viewer: Location?): BoundingBox {
        // Vanilla rotates by -yaw around Y, then pitch around X: the entity's own for fixed displays,
        // the viewer's yaw for ones that face the viewer (it only turns them, so pitch is left out)
        val yaw = if (facesViewer) viewer?.yaw ?: 0f else location.yaw
        val pitch = if (facesViewer) 0f else location.pitch
        val orientation = Quaternionf().rotationYXZ(radians(-yaw), radians(pitch), 0f)

        val matrix = Matrix4f()
            .rotation(orientation)
            .translate(transformation.translation)
            .rotate(transformation.leftRotation)
            .scale(transformation.scale)

        val low = Vector3f(Float.MAX_VALUE)
        val high = Vector3f(-Float.MAX_VALUE)
        for (x in floatArrayOf(min.x, max.x)) for (y in floatArrayOf(min.y, max.y)) for (z in floatArrayOf(min.z, max.z)) {
            val corner = matrix.transformPosition(Vector3f(x, y, z))
            low.min(corner)
            high.max(corner)
        }

        if (facesViewer && viewer == null) {
            val radius = maxOf(abs(low.x), abs(high.x), abs(low.z), abs(high.z))
            low.x = -radius; low.z = -radius
            high.x = radius; high.z = radius
        }

        return BoundingBox(
            location.x + low.x, location.y + low.y, location.z + low.z,
            location.x + high.x, location.y + high.y, location.z + high.z,
        )
    }

    // ── Glow helpers ───────────────────────────────────────────────

    /**
     * Apply glow to Display entities using setGlowColorOverride (supports full RGB).
     */
    private fun applyDisplayGlow(display: Display, glowColor: String?) {
        val color = glowColor?.let { Colors.parse(it) }
        display.isGlowing = color != null
        display.glowColorOverride = color
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

    /** A display's transformation, before any bounce. Bounds are worked out from it too. */
    private fun transformationOf(entity: BiteEntity): Transformation = when (entity) {
        is BiteBlock -> {
            val rotation = rotationOf(entity.yaw, entity.pitch)
            Transformation(blockOffset(entity.scale, rotation), rotation, Vector3f(entity.scale), Quaternionf())
        }
        is BiteItem -> Transformation(Vector3f(), rotationOf(entity.yaw, entity.pitch), Vector3f(entity.scale), Quaternionf())
        is BiteText -> Transformation(Vector3f(), Quaternionf(), Vector3f(entity.scale), Quaternionf())
        else -> Transformation(Vector3f(), Quaternionf(), Vector3f(1f), Quaternionf())
    }

    private fun rotationOf(yaw: Float, pitch: Float): Quaternionf {
        return Quaternionf().rotateY(radians(yaw)).rotateX(radians(pitch))
    }

    /**
     * Block models start at their corner. This shift centers the block on its location, resting on it,
     * so it grows from the middle and turns around its center.
     */
    private fun blockOffset(scale: Vector3f, rotation: Quaternionf): Vector3f {
        val halfModel = rotation.transform(Vector3f(scale).mul(0.5f))
        return Vector3f(0f, scale.y / 2, 0f).sub(halfModel)
    }

    private fun radians(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()
}
