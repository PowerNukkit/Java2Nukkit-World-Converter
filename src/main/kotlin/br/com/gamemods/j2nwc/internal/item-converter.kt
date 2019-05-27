package br.com.gamemods.j2nwc.internal

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtString
import java.util.concurrent.ThreadLocalRandom

internal fun NbtCompound.toNukkitInventory(nukkitInventory: NbtCompound, slotRemapper: (Int)->Int = { it }) {
    val javaItems = getNullableCompoundList("Items") ?: return
    val nukkitItems = javaItems.map { javaItem ->
        javaItem.toNukkitItem().also { nukkitItem ->
            nukkitItem["Slot"] = slotRemapper(javaItem.getByte("Slot").toInt()).toByte()
        }
    }
    nukkitInventory["Items"] = NbtList(nukkitItems)
}

internal fun NbtCompound.toNukkitItem(): NbtCompound {
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
        373, 438, 441 -> { // potions
            val potionInfo = nbt.getNullableString("Potion")?.removePrefix("minecraft:")
            if (potionInfo == null) {
                nukkitData
            } else {
                javaPotions2Bedrock[potionInfo] ?: 0
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

    nbt.getNullableStringList("CanDestroy")?.also { canDestroy ->
        val nukkitCanDestroy = NbtList(canDestroy.flatMap {tag ->
            javaBlockProps2Bedrock[tag.value]?.map { name -> NbtString(name) } ?: listOf(tag)
        })
        nukkitNbt["CanDestroy"] = nukkitCanDestroy
    }

    nbt.getNullableStringList("CanPlaceOn")?.also { canPlaceOn ->
        val nukkitCanPlaceOn = NbtList(canPlaceOn.flatMap { tag ->
            javaBlockProps2Bedrock[tag.value]?.map { name -> NbtString(name) } ?: listOf(tag)
        })
        nukkitNbt["CanPlaceOn"] = nukkitCanPlaceOn
    }

    val enchantments = nbt.getNullableCompoundList("Enchantments") ?: emptyList<NbtCompound>()
    val storedEnchantments = nbt.getNullableCompoundList("StoredEnchantments") ?: emptyList<NbtCompound>()

    (enchantments.asSequence() + storedEnchantments.asSequence())
        .mapNotNull(::convertEnch)
        .toMutableList()
        .takeIf { it.isNotEmpty() }
        ?.also { nukkitNbt["ench"] = NbtList(it) }

    nbt.copyTo(nukkitNbt, "RepairCost")

    when (nukkitId) {
        386 -> { // writable_book
            nbt.getNullableStringList("pages")?.map {
                NbtCompound("text" to it)
            }?.also {
                nukkitNbt["pages"] = NbtList(it)
            }
        }
        387 -> { // written_book
            nukkitNbt.copyFrom(nbt, "author")
            nukkitNbt.copyFrom(nbt, "title")
            nukkitNbt.copyFrom(nbt, "generation")
            nbt.getNullableStringList("pages")?.map {
                NbtCompound("text" to NbtString(it.value.fromJsonToLegacy()))
            }?.also {
                nukkitNbt["pages"] = NbtList(it)
            }
            nukkitNbt["id"] = 1095216660480L + ThreadLocalRandom.current().nextLong(0L, 2147483647L)
        }
        401, 402 -> { // firework, firework star
            fun NbtCompound.convertExplosion(): NbtCompound {
                val converted = NbtCompound()
                copyToRenaming(converted, "Flicker", "FireworkFlicker")
                copyToRenaming(converted, "Trail", "FireworkTrail")
                copyToRenaming(converted, "Type", "FireworkType")
                getNullableIntArray("Colors")?.map { it.fireworkColorToDyeColor() }?.also {
                    converted["FireworkColor"] = it.toByteArray()
                }
                getNullableIntArray("FadeColors")?.map { it.fireworkColorToDyeColor() }?.also {
                    converted["FireworkFade"] = it.toByteArray()
                }
                return converted
            }
            
            when (nukkitId) {
                401 -> { // firework
                    nbt.getNullableCompound("Fireworks")?.also { fireworks ->
                        val convertedFireworks = NbtCompound()
                        convertedFireworks.copyFrom(fireworks, "Flight")

                        fireworks.getNullableCompoundList("Explosions")?.also { explosions ->
                            convertedFireworks["Explosions"] = NbtList(
                                explosions.map { it.convertExplosion() }
                            )
                        }

                        nukkitNbt["Fireworks"] = convertedFireworks
                    }
                }
                402 -> {  // firework star
                    nbt.getNullableCompound("Explosion")?.also { explosion ->
                        explosion.getNullableIntArray("Colors")?.firstOrNull()?.also { customColor ->
                            nukkitNbt["customColor"] = customColor - 0x1000000
                        }
                        nukkitNbt["FireworksItem"] = explosion.convertExplosion() 
                    }
                }
            }
        }
    }

    if (customNukkitData != 0) {
        nukkitItem["Damage"] = customNukkitData.toShort()
    }
    if (nukkitNbt.isNotEmpty()) {
        nukkitItem["tag"] = nukkitNbt
    }
    return nukkitItem
}

internal fun convertEnch(from: NbtCompound): NbtCompound? {
    val id = javaEnchantments2Nukkit[from.getNullableString("id")?.removePrefix("minecraft:") ?: ""]
        ?.takeIf { it >= 0 }
        ?: return null

    return NbtCompound().also { to ->
        to["id"] = id.toShort()
        from.copyTo(to, "lvl")
    }
}

internal fun NbtCompound.copyToRenaming(to: NbtCompound, tagName: String, newName: String) {
    this[tagName]?.let {
        to[newName] = it
    }
}

private fun Int.fireworkColorToDyeColor(): Byte = when (this) {
    15790320 -> 15
    15435844 -> 14
    12801229 -> 13
    6719955 -> 12
    14602026 -> 11
    4312372 -> 10
    14188952 -> 9
    4408131 -> 8
    11250603 -> 7
    2651799 -> 6
    8073150 -> 5
    2437522 -> 4
    5320730 -> 3
    3887386 -> 2
    11743532 -> 1
    1973019 -> 0
    else -> 4
}
