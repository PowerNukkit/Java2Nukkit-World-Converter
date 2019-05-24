package br.com.gamemods.j2nwc

import br.com.gamemods.nbtmanipulator.NbtCompound
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.chat.ComponentSerializer

internal fun NbtCompound.copyJsonToLegacyTo(other: NbtCompound, tagName: String, defaultLegacy: String? = null) {
    val value = this.getNullableString(tagName)?.fromJsonToLegacy() ?: defaultLegacy
    if (value != null) {
        other[tagName] = value
    }
}

internal fun String.fromJsonToLegacy(): String {
    val components = ComponentSerializer.parse(this)
    val string = components.asSequence().map { component ->
        if (component.colorRaw == null) {
            component.color = ChatColor.RESET
        }
        component.toLegacyText()
    }.joinToString().removePrefix("\u00A7r")
    return string
}
