package br.com.gamemods.nbtmanipulator

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.chat.ComponentSerializer
import java.util.*
import kotlin.Comparator

fun Region.toNukkit(): Region {
    return Region(position, values.map { Chunk(it.lastModified, it.toNukkit().toNbt()) })
}

fun Chunk.toNukkit(): NukkitChunk {
    val javaChunk = JavaChunk(this)
    val javaTileEntities = javaChunk.tileEntities.value.associate {
        BlockPos(it.getInt("x"), it.getInt("y"), it.getInt("z")) to it
    }
    val nukkitTileEntities = mutableMapOf<BlockPos, NbtCompound>()
    val nukkitSections = javaChunk.sections.entries.asSequence()
        .filter { it.value.yPos >= 0 }
        .mapNotNull { it.key to (it.value.toNukkit(javaTileEntities, nukkitTileEntities) ?: return@mapNotNull null) }
        .toMap()
    val nukkitChunk = NukkitChunk(
        NbtList(javaChunk.entities.value.map { toNukkitEntity(it) }),
        nukkitSections,
        NbtList(nukkitTileEntities.values.toMutableList()),
        javaChunk.inhabitedTime,
        false,
        javaChunk.sections.isNotEmpty(),
        javaChunk.sections.isNotEmpty(),
        1,
        javaChunk.position,
        toNukkitBiomes(javaChunk.biomes),
        byteArrayOf(0, 0, 0, 0),
        IntArray(256) {
            64
        }
    )
    return nukkitChunk
}

data class BlockPos(val xPos: Int, val yPos: Int, val zPos: Int)
data class JavaBlock(val blockPos: BlockPos, val type: JavaPalette, var tileEntity: NbtCompound?)
data class NukkitBlock(val blockPos: BlockPos, var blockData: BlockData, var tileEntity: NbtCompound?)
fun JavaChunkSection.toNukkit(javaTileEntities: Map<BlockPos, NbtCompound>, nukkitTileEntities: MutableMap<BlockPos, NbtCompound>): NukkitChunkSection? {
    val blockStates = blockStates ?: return NukkitChunkSection(
        yPos = yPos,
        blockLight = ByteArray(2048),
        blocks = ByteArray(4096),
        blockData = ByteArray(2048),
        skyLight = skyLight ?: ByteArray(2048)
    )

    val palette = palette ?: emptyList()

    val bitPerIndex = blockStates.size * 64 / 4096
    val maxValue = (1L shl bitPerIndex) - 1L

    fun get(pos: Int): Int {
        val a = pos * bitPerIndex
        val b = a shr 6
        val c = (pos + 1) * bitPerIndex - 1 shr 6
        val d = a xor (b shl 6)
        return if (b == c) {
            (blockStates[b].ushr(d) and maxValue).toInt()
        } else {
            val e = 64 - d
            (blockStates[b].ushr(d) or (blockStates[c] shl e) and maxValue).toInt()
        }
    }

    val paletteIndexes = IntArray(4096) { i ->
        get(i).also {
            check(it >= 0 && it < palette.size) {
                "Failed to get the Palette ID"
            }
        }
    }

    val blockPalettes = Array(4096) {
        val y = (it shr 8) and 0xF
        val z = (it shr 4) and 0xF
        val x = it and 0xF
        val blockPos = BlockPos(x + chunkPos.xPos * 16, y + yPos * 16, z + chunkPos.zPos * 16)
        JavaBlock(blockPos, palette[paletteIndexes[it]], javaTileEntities[blockPos])
    }

    val javaBlocks = blockPalettes.associate { it.blockPos to it }

    val nukkitBlocks = blockPalettes.map { it.toNukkit(javaBlocks).also {block ->
        block.tileEntity?.let { nukkitTileEntity ->
            nukkitTileEntities[block.blockPos] = nukkitTileEntity
        }
    } }

    return NukkitChunkSection(
        yPos = yPos,
        blockLight = ByteArray(2048),
        blocks = ByteArray(4096) { nukkitBlocks[it].blockData.blockId },
        blockData = ByteArray(2048) {
            val double = it * 2
            val stored = nukkitBlocks[double].blockData.data to nukkitBlocks[double + 1].blockData.data
            val first = stored.first.toInt() and 0x0F
            val second = (stored.second.toInt() and 0x0F) shl 4
            val merged = first or second
            (merged and 0xFF).toByte()
        },
        skyLight = skyLight ?: ByteArray(2048)
    )
}

fun toNukkitTileEntity(javaEntity: NbtCompound): NbtCompound? {
    return null
}

fun toNukkitEntity(javaEntity: NbtCompound): NbtCompound {
    return javaEntity
}

fun toNukkitBiomes(biomes: IntArray): ByteArray {
    return biomes.map { it.toByte() }.toByteArray()
}

val java2bedrockStates = Properties().apply {
    JavaPalette::class.java.getResourceAsStream("/block-states.properties").bufferedReader().use {
        load(it)
    }
}.mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString() }

val bedrock2nukkit = Properties().apply {
    JavaPalette::class.java.getResourceAsStream("/bedrock-2-nukkit.properties").bufferedReader().use {
        load(it)
    }
}

val java2bedrockItems = Properties().apply {
    JavaPalette::class.java.getResourceAsStream("/items.properties").bufferedReader().use {
        load(it)
    }
}.mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString() }

val nukkitBlockIds = Properties().apply {
    JavaPalette::class.java.getResourceAsStream("/nukkit-block-ids.properties").bufferedReader().use {
        load(it)
    }
}.mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString().toInt() }

val nukkitItemIds = Properties().apply {
    JavaPalette::class.java.getResourceAsStream("/nukkit-item-ids.properties").bufferedReader().use {
        load(it)
    }
}.mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString().toInt() }

val nukkitBlockNames = nukkitBlockIds.entries.asSequence()
    .map { (k,v) -> k to v }.groupBy { (_,v) -> v }
    .mapValues { it.value.map { p-> p.first } }

val nukkitItemNames = nukkitItemIds.entries.asSequence()
    .map { (k,v) -> k to v }.groupBy { (_,v) -> v }
    .mapValues { it.value.map { p-> p.first } }
    .let {
        mapOf(0 to "air") + it
    }

object IdComparator: Comparator<Map.Entry<String, String>> {
    override fun compare(entry1: Map.Entry<String, String>, entry2: Map.Entry<String, String>): Int {
        val (blockId1, blockData1) = entry1.value.split(',', limit = 2).map { it.toInt() }
        val (blockId2, blockData2) = entry2.value.split(',', limit = 2).map { it.toInt() }
        return blockId1.compareTo(blockId2).takeIf { it != 0 }
            ?: blockData1.compareTo(blockData2).takeIf { it != 0 }
            ?: entry1.key.compareTo(entry2.key)
    }
}

object TypeIdComparator: Comparator<Map.Entry<String, String>> {
    override fun compare(entry1: Map.Entry<String, String>, entry2: Map.Entry<String, String>): Int {
        val (type1, blockId1, blockData1) = entry1.value.split(',', limit = 3)
        val (type2, blockId2, blockData2) = entry2.value.split(',', limit = 3)
        return type1.compareTo(type2).takeIf { it != 0 }
            ?: blockId1.toInt().compareTo(blockId2.toInt()).takeIf { it != 0 }
            ?: (blockData1.takeIf { it != "~" }?.toInt() ?: -1)
                .compareTo(blockData2.takeIf { it != "~" }?.toInt() ?: -1).takeIf { it != 0 }
            ?: entry1.key.compareTo(entry2.key)
    }
}

fun checkIds() {
    val validBlockPattern = Regex("^\\d+,\\d+$")
    java2bedrockStates.values.find { !validBlockPattern.matches(it) }?.let {
        error("Found an invalid mapping at block-states.properties: $it")
    }


    val validKeyPattern = Regex("^(B,\\d+,\\d+)|(I,\\d+,(\\d+|~))$")
    java2bedrockItems.values.find { !validKeyPattern.matches(it) }?.let {
        error("Found an invalid mapping at items.properties: $it")
    }

    val validItemValuePattern = Regex("^\\d+,(\\d+|~)$")
    bedrock2nukkit.forEach { k, v ->
        val key = k.toString()
        if (!validKeyPattern.matches(key)) {
            error("Found an invalid key at bedrock-2-nukkit.properties: $key")
        }

        if (key[0] == 'B') {
            if (!validBlockPattern.matches(v.toString())) {
                error("Found an invalid value at bedrock-2-nukkit.properties: Key:$key Value:$v")
            }
        } else {
            if (!validItemValuePattern.matches(v.toString())) {
                error("Found an invalid value at bedrock-2-nukkit.properties: Key:$key Value:$v")
            }
        }
    }

    java2bedrockStates.asSequence().sortedWith(IdComparator).forEach { (state, stateMapping) ->
        val (mappedBlockId, mappedBlockData) = stateMapping.split(',', limit = 2).map { it.toInt() }
        val (nukkitBlockId, nukkitBlockData) =
            (bedrock2nukkit["B,$mappedBlockId,$mappedBlockData"]?.toString() ?: stateMapping)
                .split(',', limit = 2).map { it.toInt() }

        if (nukkitBlockId !in nukkitBlockNames) {
            error("The block $nukkitBlockId,$nukkitBlockData is unsupported by Nukkit!\nState: $state")
        }

        if (nukkitBlockData !in 0..15) {
            error("The block $nukkitBlockId,$nukkitBlockData has data out of range 0..15!\nState: $state")
        }
    }

    java2bedrockItems.asSequence().sortedWith(TypeIdComparator).forEach { (item, stateMapping) ->
        val (type, mappedItemId, mappedItemData) = stateMapping.split(',', limit = 3)
        val (nukkitItemId, nukkitItemData) =
            (bedrock2nukkit["$type,$mappedItemId,$mappedItemData"]?.toString()
                ?: bedrock2nukkit["$type,$mappedItemId,~"]?.toString()
                ?: "$mappedItemId,$mappedItemData")
                .split(',', limit = 2).mapIndexed { i, str->
                    when {
                        i == 0 -> str
                        str == "~" -> mappedItemData
                        else -> str
                    }
                }

        if (type == "I") {
            if (nukkitItemId.toInt() !in nukkitItemNames) {
                error("The item $type,$nukkitItemId,$nukkitItemData is unsupported by Nukkit!\nItem: $item")
            }
        } else {
            if (nukkitItemId.toInt() !in nukkitBlockNames) {
                error("The item-block $type,$nukkitItemId,$nukkitItemData is unsupported by Nukkit!\nItem: $item")
            }
        }
    }
}

fun JavaBlock.toNukkit(javaBlocks: Map<BlockPos, JavaBlock>): NukkitBlock {
    val blockData = this.type.toNukkit()

    fun commonBlockEntityData(id: String) = arrayOf(
        "id" to NbtString(id),
        "x" to NbtInt(blockPos.xPos),
        "y" to NbtInt(blockPos.yPos),
        "z" to NbtInt(blockPos.zPos),
        "isMoveable" to NbtByte(false)
    )

    fun createTileEntity(id: String, vararg tags: Pair<String, NbtTag>, action: (NbtCompound)->Unit = {}): NbtCompound {
        return NbtCompound(*commonBlockEntityData(id), *tags).also(action)
    }

    val nukkitTileEntity = when (blockData.blockId.toInt()) {
        // Bed
        26 -> createTileEntity("Bed",
            "color" to NbtByte(when (type.blockName) {
                "minecraft:white_bed" -> 0
                "minecraft:orange_bed" -> 1
                "minecraft:magenta_bed" -> 2
                "minecraft:light_blue_bed" -> 3
                "minecraft:yellow_bed" -> 4
                "minecraft:lime_bed" -> 5
                "minecraft:pink_bed" -> 6
                "minecraft:gray_bed" -> 7
                "minecraft:light_gray_bed" -> 8
                "minecraft:cyan_bed" -> 9
                "minecraft:purple_bed" -> 10
                "minecraft:blue_bed" -> 11
                "minecraft:brown_bed" -> 12
                "minecraft:green_bed" -> 13
                "minecraft:red_bed" -> 14
                "minecraft:black_bed" -> 15
                else -> 14
            })
        )
        // Chest
        54 -> createTileEntity("Chest") { nukkitEntity ->
            //TODO Convert items from the chest
            tileEntity?.copyJsonToLegacyTo(nukkitEntity, "CustomName")
            val pair = when (type.properties?.getString("type")) {
                "left" -> when (type.properties?.getString("facing")) {
                    "east" -> blockPos.xPos to blockPos.zPos +1
                    "south" -> blockPos.xPos -1 to blockPos.zPos
                    "west" -> blockPos.xPos to blockPos.zPos -1
                    else -> blockPos.xPos +1 to blockPos.zPos
                }
                "right" -> when (type.properties?.getString("facing")) {
                    "east" -> blockPos.xPos to blockPos.zPos -1
                    "south" -> blockPos.xPos +1 to blockPos.zPos
                    "west" -> blockPos.xPos to blockPos.zPos +1
                    else -> blockPos.xPos -1 to blockPos.zPos
                }
                else -> null
            }
            pair?.let { (x, z) ->
                nukkitEntity["pairx"] = x
                nukkitEntity["pairz"] = z
            }
            tileEntity?.toNukkitInventory(nukkitEntity)
        }
        // Furnance
        61, 62 -> createTileEntity("Furnace") { nukkitEntity ->
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                copyTo(nukkitEntity, "BurnTime")
                copyTo(nukkitEntity, "CookTime")
                nukkitEntity["BurnDuration"] = getShort("CookTimeTotal")
                toNukkitInventory(nukkitEntity)
            }
        }
        // Signs
        63,436,441,443,445,447,68,437,442,444,446,448,323,472,473,474,475,476 ->
            createTileEntity("Sign") { nukkitEntity ->
                for (i in 1..4) {
                    val text = tileEntity?.getString("Text$i")?.fromJsonToLegacy() ?: ""
                    nukkitEntity["Text$i"] = text
                }
            }
        else -> tileEntity?.let { toNukkitTileEntity(it) }
    }

    return NukkitBlock(blockPos, blockData, nukkitTileEntity)
}

fun NbtCompound.copyJsonToLegacyTo(other: NbtCompound, tagName: String, defaultLegacy: String? = null) {
    val value = this.getNullableString(tagName)?.fromJsonToLegacy() ?: defaultLegacy
    if (value != null) {
        other[tagName] = value
    }
}

fun String.fromJsonToLegacy(): String {
    val components = ComponentSerializer.parse(this)
    val string = components.asSequence().map { component ->
        if (component.colorRaw == null) {
            component.color = ChatColor.RESET
        }
        component.toLegacyText()
    }.joinToString().removePrefix("\u00A7r")
    return string
}

fun NbtCompound.toNukkitInventory(nukkitInventory: NbtCompound) {
    val javaItems = getNullableCompoundList("Items") ?: return
    val nukkitItems = javaItems.value.map { javaItem ->
        javaItem.toNukkitItem().also { nukkitItem ->
            nukkitItem.copyFrom(javaItem, "Slot")
        }
    }
    nukkitInventory["Items"] = NbtList(nukkitItems)
}

fun NbtCompound.toNukkitItem(): NbtCompound {
    val nukkitItem = NbtCompound()
    nukkitItem.copyFrom(this, "Count")
    val javaId = getString("id")
    val nbt = getNullableCompound("tag")
    val damage = nbt?.getNullableInt("Damage") ?: 0
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

    if (nukkitData != 0) {
        nukkitItem["Damage"] = nukkitData.toShort()
    }
    if (nbt != null) {
        nukkitItem["tag"] = nbt
    }
    return nukkitItem
}

data class BlockData(val blockId: Byte, val data: Byte)
fun JavaPalette.toNukkit(): BlockData {
    val propertiesId = properties?.value
        ?.mapValuesTo(TreeMap()) { (it.value as NbtString).value }
        ?.map { "${it.key}-${it.value}" }
        ?.joinToString(";", prefix = ";")
        ?: ""

    val stateId = "$blockName$propertiesId".removePrefix("minecraft:").replace(':', '-').toLowerCase()

    val prop = java2bedrockStates[stateId] ?: java2bedrockStates[blockName] ?: "1,15"
    val nukkit = bedrock2nukkit.getProperty("B,$prop") ?: prop
    val ids = nukkit.split(',', limit = 2)
    val blockId = ids[0].toInt()
    val blockData = ids.getOrElse(1) { "0" }.toByte()
    check(blockId in 0..255) {
        "Block id unsupported by Nukkit: $blockId:$blockData"
    }

    return BlockData((blockId and 0xFF).toByte(), blockData)
}
