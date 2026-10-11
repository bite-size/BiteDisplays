package gg.bitesize.displays.commands

import gg.bitesize.displays.models.BiteType

object Permissions {
    const val USE = "bitedisplays.use"
    const val NPC = "bitedisplays.npc"
    const val ITEM = "bitedisplays.item"
    const val BLOCK = "bitedisplays.block"
    const val TEXT = "bitedisplays.text"
    const val MANAGE = "bitedisplays.manage"
    const val ADMIN = "bitedisplays.admin"

    /** The permission to create and edit entities of [type]. */
    fun forType(type: BiteType): String = when (type) {
        BiteType.NPC -> NPC
        BiteType.ITEM -> ITEM
        BiteType.BLOCK -> BLOCK
        BiteType.TEXT -> TEXT
    }
}
