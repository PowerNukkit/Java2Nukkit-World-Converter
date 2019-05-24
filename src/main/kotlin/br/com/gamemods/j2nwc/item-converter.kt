package br.com.gamemods.j2nwc

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtString
import java.util.concurrent.ThreadLocalRandom

fun NbtCompound.toNukkitInventory(nukkitInventory: NbtCompound, slotRemapper: (Int)->Int = { it }) {
    val javaItems = getNullableCompoundList("Items") ?: return
    val nukkitItems = javaItems.value.map { javaItem ->
        javaItem.toNukkitItem().also { nukkitItem ->
            nukkitItem["Slot"] = slotRemapper(javaItem.getByte("Slot").toInt()).toByte()
        }
    }
    nukkitInventory["Items"] = NbtList(nukkitItems)
}

fun NbtCompound.toNukkitItem(): NbtCompound {
    val nukkitItem = NbtCompound()
    nukkitItem.copyFrom(this, "Count")
    val javaId = getString("id")
    val nbt = getNullableCompound("tag") ?: NbtCompound()
    val damage = nbt.getNullableInt("Damage") ?: 0
    val internalId = javaId.removePrefix("minecraft:").toLowerCase()
    val bedrockMapping = java2bedrockItems[internalId] ?: "B,0,0"
    val (type, bedrockId, rawBedrockData) = bedrockMapping.split(',', limit = 3)
    val bedrockData = rawBedrockData.takeUnless { it == "~" }?.toInt() ?: damage
    val nukkitMapping = bedrock2nukkit.getProperty("$type,$bedrockId,$bedrockData")
        ?: bedrock2nukkit.getProperty("$type,$bedrockId,$rawBedrockData")
        ?: "$bedrockId,$bedrockData"
    val (rawNukkitId, rawNukkitData) = nukkitMapping.split(',', limit = 2)
    val nukkitData = rawNukkitData.takeUnless { it == "~" }?.toInt() ?: damage

    val nukkitId = if (type == "B" && rawNukkitId.toInt() > 255) {
        255 - rawNukkitId.toInt()
    } else {
        rawNukkitId.toInt()
    }

    nukkitItem["id"] = nukkitId.toShort()

    val customNukkitData = when (nukkitId) {
        383 -> { // spawn_egg
            val entity = javaId.removeSuffix("_spawn_egg").removePrefix("minecraft:")
            java2bedrockEntities[entity] ?: 0
        }
        262 -> { // arrow and tipped_arrow
            val potionInfo = nbt.getNullableString("Potion")?.removePrefix("minecraft:")
            if (potionInfo == null) {
                nukkitData
            } else {
                tippedArrows[potionInfo] ?: 0
            }
        }
        else -> nukkitData
    }

    val nukkitNbt = NbtCompound()
    nukkitNbt.copyFrom(nbt, "Unbreakable")
    nukkitNbt.copyFrom(nbt, "HideFlags")
    nbt.getNullableCompound("display")?.also { display ->
        val nukkitDisplay = NbtCompound()
        nukkitNbt["display"] = nukkitDisplay
        display.copyJsonToLegacyTo(nukkitDisplay, "Name")
        nukkitDisplay.copyFrom(display, "Lore")
        display.getNullableInt("color")?.let {
            nukkitNbt["customColor"] = it
        }
    }

    nbt.getNullableStringList("CanDestroy")?.value?.also { canDestroy ->
        val nukkitCanDestroy = NbtList(canDestroy.flatMap {tag ->
            javaBlockProps2Bedrock[tag.value]?.map { name -> NbtString(name) } ?: listOf(tag)
        })
        nukkitNbt["CanDestroy"] = nukkitCanDestroy
    }

    nbt.getNullableStringList("CanPlaceOn")?.value?.also { canPlaceOn ->
        val nukkitCanPlaceOn = NbtList(canPlaceOn.flatMap { tag ->
            javaBlockProps2Bedrock[tag.value]?.map { name -> NbtString(name) } ?: listOf(tag)
        })
        nukkitNbt["CanPlaceOn"] = nukkitCanPlaceOn
    }

    when (nukkitId) {
        386 -> { // writable_book
            nbt.getNullableStringList("pages")?.value?.map {
                NbtCompound("text" to it)
            }?.also {
                nukkitNbt["pages"] = NbtList(it)
            }
        }
        387 -> { // written_book
            nukkitNbt.copyFrom(nbt, "author")
            nukkitNbt.copyFrom(nbt, "title")
            nukkitNbt.copyFrom(nbt, "generation")
            nbt.getNullableStringList("pages")?.value?.map {
                NbtCompound("text" to NbtString(it.value.fromJsonToLegacy()))
            }?.also {
                nukkitNbt["pages"] = NbtList(it)
            }
            nukkitNbt["id"] = 1095216660480L + ThreadLocalRandom.current().nextLong(0L, 2147483647L)
        }
    }

    if (customNukkitData != 0) {
        nukkitItem["Damage"] = customNukkitData.toShort()
    }
    if (nukkitNbt.value.isNotEmpty()) {
        nukkitItem["tag"] = nukkitNbt
    }
    return nukkitItem
}
