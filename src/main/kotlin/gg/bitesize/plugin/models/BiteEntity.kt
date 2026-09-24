package gg.bitesize.plugin.models

import org.bukkit.Location
import java.util.UUID

interface BiteEntity {
    val id: UUID
    val location: Location
}