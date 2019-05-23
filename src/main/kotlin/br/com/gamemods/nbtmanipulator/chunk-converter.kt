package br.com.gamemods.nbtmanipulator

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.chat.ComponentSerializer
import java.util.*
import java.util.concurrent.ThreadLocalRandom
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
        NbtList(javaChunk.entities.value.mapNotNull { toNukkitEntity(it, javaChunk, nukkitSections, nukkitTileEntities) }),
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
        blocks = ByteArray(4096) { nukkitBlocks[it].blockData.byteBlockId },
        blockData = ByteArray(2048) {
            val double = it * 2
            val stored = nukkitBlocks[double].blockData.data to nukkitBlocks[double + 1].blockData.data
            val first = stored.first and 0x0F
            val second = (stored.second and 0x0F) shl 4
            val merged = first or second
            (merged and 0xFF).toByte()
        },
        skyLight = skyLight ?: ByteArray(2048)
    )
}

fun toNukkitTileEntity(javaEntity: NbtCompound): NbtCompound? {
    return null
}

fun toNukkitEntity(
    javaEntity: NbtCompound,
    javaChunk: JavaChunk,
    nukkitSections: Map<Int, NukkitChunkSection>,
    nukkitTileEntities: MutableMap<BlockPos, NbtCompound>
): NbtCompound? {
    fun convertBaseEntity(): NbtCompound? {
        return NbtCompound().apply {
            val nukkitId = javaEntities2Nukkit[javaEntity.getNullableString("id")?.removePrefix("minecraft:") ?: return null] ?: return null
            this["id"] = nukkitId
            copyFrom(javaEntity, "CustomName")
            copyFrom(javaEntity, "CustomNameVisible")
            copyFrom(javaEntity, "Pos")
            copyFrom(javaEntity, "Motion")
            copyFrom(javaEntity, "Rotation")
            copyFrom(javaEntity, "FallDistance")
            copyFrom(javaEntity, "Fire")
            copyFrom(javaEntity, "Air")
            copyFrom(javaEntity, "OnGround")
            copyFrom(javaEntity, "Invulnerable")
            copyFrom(javaEntity, "Scale")
            copyFrom(javaEntity, "Silent")
            copyFrom(javaEntity, "NoGravity")
            copyFrom(javaEntity, "Glowing")
            //TODO Scoreboard Tags
            //TODO Passengers
        }
    }
    return when(javaEntity.getString("id").removePrefix("minecraft:")) {
        "painting" -> {
            val nukkitEntity = convertBaseEntity() ?: return null
            val paintingData = paintings[javaEntity.getNullableString("Motive")?.removePrefix("minecraft:") ?: ""] ?: return null
            var (posX, posY, posZ) = nukkitEntity.getDoubleList("Pos").value.map { it.value }
            nukkitEntity.copyFrom(javaEntity, "TileX")
            nukkitEntity.copyFrom(javaEntity, "TileY")
            nukkitEntity.copyFrom(javaEntity, "TileZ")
            val facing = javaEntity.getNullableByte("Facing")
            val direction: Byte = when (facing?.toInt()) {
                0 -> 2
                1 -> 2
                2 -> 2
                3 -> 2
                null -> 2
                else -> 2
            }
            nukkitEntity["Direction"] = direction
            nukkitEntity["Rotation"] = NbtList(NbtFloat(direction.toFloat()), NbtFloat(0F))
            nukkitEntity["Motive"] = paintingData.id
            nukkitEntity
            null //TODO Complete this
        }
        "item_frame" -> {
            val tileX = javaEntity.getInt("TileX")
            val tileY = javaEntity.getInt("TileY")
            val tileZ = javaEntity.getInt("TileZ")
            val chunkSectionY = tileY / 16
            val chunkSection = nukkitSections[chunkSectionY] ?: return null
            val internalX = tileX % 32
            val internalY = tileY % 32
            val internalZ = tileZ % 32
            val offset = ((internalY and 0xF) shl 8) + ((internalZ and 0xF) shl 4) + (internalX and 0xF)
            val blockIdInPosition = chunkSection.blocks[offset].toInt()
            // Item Frame in Bedrock Edition is a block and not an entity, check if we aren't replacing any block.
            if (blockIdInPosition != 0) {
                return null
            }
            val facing = when (javaEntity.getByte("Facing").toInt()) {
                0, 1 -> return null // Facing down and up is unsupported by Bedrock Edition
                2 -> 3
                3 -> 2
                4 -> 5
                5 -> 4
                else -> 3
            }
            chunkSection.blocks[offset] = (199 and 0xFF).toByte()
            val halfOffset = offset / 2
            val isSecond = offset % 2 == 1
            val stored = if (!isSecond) {
                facing to (chunkSection.blockData[halfOffset].toInt() and 0x0F)
            } else {
                ((chunkSection.blockData[halfOffset].toInt() and 0xF0) shr 4) to facing
            }
            val first = stored.first and 0x0F
            val second = (stored.second and 0x0F) shl 4
            val merged = first or second
            chunkSection.blockData[halfOffset] = (merged and 0xFF).toByte()
            val tileEntity = NbtCompound(
                "id" to NbtString("ItemFrame"),
                "x" to NbtInt(tileX),
                "y" to NbtInt(tileY),
                "z" to NbtInt(tileZ),
                "isMoveable" to NbtByte(false)
            )
            tileEntity.copyFrom(javaEntity, "ItemDropChance")
            tileEntity.copyFrom(javaEntity, "ItemRotation")
            javaEntity.getNullableCompound("Item")?.toNukkitItem()?.let {
                tileEntity["Item"] = it
            }
            nukkitTileEntities[BlockPos(tileX, tileY, tileZ)] = tileEntity
            null
        }
        else -> null
    }
}

fun toNukkitBiomes(biomes: IntArray): ByteArray {
    return biomes.map { it.toByte() }.toByteArray()
}

private fun properties(name: String) = Properties().apply {
    JavaPalette::class.java.getResourceAsStream(name).bufferedReader().use {
        load(it)
    }
}

private fun propertiesStringString(name: String) = properties(name)
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString() }

private fun propertiesStringInt(name: String) = properties(name)
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString().toInt() }

val java2bedrockEntities = propertiesStringInt("/entity-ids.properties")

val java2bedrockStates = propertiesStringString("/block-states.properties")

val bedrock2nukkit = properties("/bedrock-2-nukkit.properties")

val java2bedrockItems = propertiesStringString("/items.properties")

val nukkitBlockIds = propertiesStringInt("/nukkit-block-ids.properties")

val nukkitItemIds = propertiesStringInt("/nukkit-item-ids.properties")

val nukkitBlockNames = nukkitBlockIds.entries.asSequence()
    .map { (k,v) -> k to v }.groupBy { (_,v) -> v }
    .mapValues { it.value.map { p-> p.first } }

val nukkitItemNames = nukkitItemIds.entries.asSequence()
    .map { (k,v) -> k to v }.groupBy { (_,v) -> v }
    .mapValues { it.value.map { p-> p.first } }
    .let {
        mapOf(0 to "air") + it
    }

val javaStatusEffectNames = properties("/status-effect-java-ids.properties")
    .mapKeys { it.key.toString().toInt() }.mapValues { it.value.toString().toLowerCase() }

val javaStatusEffectIds = javaStatusEffectNames.entries.associate { it.value to it.key }
val java2bedrockEffectIds = propertiesStringInt("/status-effect-ids.properties")

val javaTags = properties("/tags.properties")
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { entry ->
        entry.value.toString().split(',').map { it.trim() }
    }.let { tags2bedrock ->
        val mutable = tags2bedrock.toMutableMap()
        while (mutable.values.any { list-> list.any { it.startsWith("#") } }) {
            mutable.iterator().forEach { entry ->
                if (entry.value.any { it.startsWith('#') }) {
                    entry.setValue(entry.value.flatMap {
                        if (it.startsWith('#')) {
                            (mutable[it.substring(1)]?.asSequence() ?: sequenceOf()).asIterable()
                        } else {
                            sequenceOf(it).asIterable()
                        }
                    }.toSet().toList())
                }
            }
        }
        mutable
    }

val javaTags2Bedrock = javaTags.mapValues { entry ->
    entry.value.asSequence().flatMap { javaBlock ->
        val (bedrockId, bedrockData) = java2bedrockStates[javaBlock]?.split(',', limit = 2) ?: listOf("0","0").also {
            println("The tag ${entry.key} points to a missing block $javaBlock")
        }
        val (nukkitId, _) = (
                bedrock2nukkit.getProperty("B,$bedrockId,$bedrockData") ?: "$bedrockId,$bedrockData"
            ).split(',', limit = 2)
        if (nukkitId != "0") {
            nukkitBlockNames[nukkitId.toInt()]?.asSequence() ?: sequenceOf(nukkitId, javaBlock)
        } else {
            sequenceOf(null)
        }
    }.filterNotNull().toList()
}

val javaBlockProps2Bedrock = javaTags2Bedrock + java2bedrockStates.asSequence().filter { ';' !in it.key }.flatMap { (javaBlock, mapping) ->
    val (bedrockId, bedrockData) = mapping.split(',', limit = 2)
    val (nukkitId, _) = (
            bedrock2nukkit.getProperty("B,$bedrockId,$bedrockData") ?: "$bedrockId,$bedrockData"
            ).split(',', limit = 2)
    if (nukkitId != "0") {
        sequenceOf(javaBlock to (nukkitBlockNames[nukkitId.toInt()] ?: listOf(nukkitId, javaBlock)))
    } else {
        sequenceOf(null)
    }
}.filterNotNull()

val tippedArrows = propertiesStringInt("/tipped-arrows.properties")

data class PaintingData(val id: String, val width: Int, val height: Int)
val paintings = propertiesStringString("/paintings.properties")
    .mapValues { (_, value) ->
        val (id, width, height) = value.split(',', limit = 3)
        PaintingData(id, width.toInt(), height.toInt())
    }
val javaEntities2Nukkit = propertiesStringString("/java-entities.properties")

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
                println("The item $type,$nukkitItemId,$nukkitItemData is unsupported by Nukkit!\nItem: $item")
            }
        } else {
            if (nukkitItemId.toInt() !in nukkitBlockNames) {
                println("The item-block $type,$nukkitItemId,$nukkitItemData is unsupported by Nukkit!\nItem: $item")
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

    val nukkitTileEntity = when (blockData.blockId) {
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
        54 -> createTileEntity("Chest") { nukkitEntity ->
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
        130 -> createTileEntity("EnderChest")
        61, 62 -> createTileEntity("Furnace") { nukkitEntity ->
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                copyTo(nukkitEntity, "BurnTime")
                copyTo(nukkitEntity, "CookTime")
                nukkitEntity["BurnDuration"] = getShort("CookTimeTotal")
                toNukkitInventory(nukkitEntity)
            }
        }
        117 -> createTileEntity("BrewingStand") { nukkitEntity ->
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                nukkitEntity["CookTime"] = getShort("BrewTime")
                val fuel = getByte("Fuel")
                val fuelTotal = if (fuel > 0) 20 else 0
                nukkitEntity["FuelTotal"] = fuelTotal.toShort()
                nukkitEntity["FuelAmount"] = fuel.toShort()
                toNukkitInventory(nukkitEntity) {
                    when {
                        it == 3 -> 0
                        it < 3 -> it + 1
                        else -> it
                    }
                }
            }
        }
        151, 178 -> createTileEntity("DaylightDetector")
        25 -> createTileEntity("Music") { nukkitEntity ->
            nukkitEntity["note"] = type.properties?.getString("note")?.toByte() ?: 0
            nukkitEntity["powered"] = type.properties?.getString("powered")?.toBoolean() ?: false
        }
        63,436,441,443,445,447,68,437,442,444,446,448,323,472,473,474,475,476 ->
            createTileEntity("Sign") { nukkitEntity ->
                for (i in 1..4) {
                    val text = tileEntity?.getString("Text$i")?.fromJsonToLegacy() ?: ""
                    nukkitEntity["Text$i"] = text
                }
            }
        52 -> createTileEntity("MobSpawner") { nukkitEntity ->
            // Tile entity based on
            // https://github.com/Nukkit-coders/MobPlugin/blob/master/src/main/java/nukkitcoders/mobplugin/entities/block/BlockEntitySpawner.java
            tileEntity?.apply {
                copyTo(nukkitEntity, "SpawnRange")
                copyTo(nukkitEntity, "MinSpawnDelay")
                copyTo(nukkitEntity, "MaxSpawnDelay")
                copyTo(nukkitEntity, "MaxNearbyEntities")
                copyTo(nukkitEntity, "RequiredPlayerRange")
                nukkitEntity["EntityId"] = getNullableCompound("SpawnData")?.entityToId() ?: 12
            }
        }
        116 -> createTileEntity("EnchantTable") { nukkitEntity ->
            tileEntity?.copyJsonToLegacyTo(nukkitEntity, "CustomName")
        }
        144 -> createTileEntity("Skull") { nukkitEntity ->
            val rotation = type.properties?.getNullableString("rotation") ?: "0"
            val type = when (type.blockName.removePrefix("minecraft:")) {
                "skeleton_skull", "skeleton_wall_skull" -> 0
                "wither_skeleton_skull", "wither_skeleton_wall_skull" -> 1
                "zombie_head", "zombie_wall_head" -> 2
                "player_head", "player_wall_head" -> 3
                "creeper_head", "creeper_wall_head" -> 4
                else -> 0
            }
            nukkitEntity["Rot"] = rotation.toByte()
            nukkitEntity["SkullType"] = type.toByte()
            if ("_wall_" !in this.type.blockName) {
                blockData.data = 1
            }
        }
        140 -> createTileEntity("FlowerPot") { nukkitEntity ->
            val potted = when (type.blockName.removePrefix("minecraft:")) {
                "potted_oak_sapling" -> BlockData(6,0)
                "potted_spruce_sapling" -> BlockData(6,1)
                "potted_birch_sapling" -> BlockData(6,2)
                "potted_jungle_sapling" -> BlockData(6,3)
                "potted_acacia_sapling" -> BlockData(6,4)
                "potted_dark_oak_sapling" -> BlockData(6,5)
                "potted_fern" -> BlockData(31,1)
                "potted_dandelion" -> BlockData(37,0)
                "potted_poppy" -> BlockData(38,0)
                "potted_blue_orchid" -> BlockData(38,1)
                "potted_allium" -> BlockData(38,2)
                "potted_azure_bluet" -> BlockData(38,3)
                "potted_red_tulip" -> BlockData(38,4)
                "potted_orange_tulip" -> BlockData(38,5)
                "potted_white_tulip" -> BlockData(38,6)
                "potted_pink_tulip" -> BlockData(38,7)
                "potted_oxeye_daisy" -> BlockData(38,8)
                "potted_cornflower" -> BlockData(38,9)
                "potted_lily_of_the_valley" -> BlockData(38,10)
                "potted_wither_rose" -> BlockData(38,2)
                "potted_brown_mushroom" -> BlockData(39,0)
                "potted_red_mushroom" -> BlockData(40,0)
                "potted_dead_bush" ->BlockData(32,0)
                "potted_cactus" ->BlockData(81,0)
                "potted_bamboo" -> BlockData(38,0)
                else -> BlockData(0,0)
            }
            potted.blockId.takeIf { it != 0 }?.let {
                nukkitEntity["item"] = it
            }

            potted.data.takeIf { it != 0 }?.let {
                nukkitEntity["data"] = it
            }
        }
        118 -> createTileEntity("Cauldron")
        138 -> createTileEntity("Beacon") { nukkitEntity ->
            tileEntity?.apply {
                val primary = javaStatusEffectNames[getNullableInt("Primary") ?: 0]?.let { java2bedrockEffectIds[it] }
                val secondary = javaStatusEffectNames[getNullableInt("Secondary") ?: 0]?.let { java2bedrockEffectIds[it] }
                primary?.let { nukkitEntity["Primary"] = it }
                secondary?.let { nukkitEntity["Secondary"] = it }
            }
        }
        33, 29 -> createTileEntity("PistonArm") { nukkitEntity ->
            val sticky = when (type.blockName) {
                "minecraft:piston" -> false
                "minecraft:sticky_piston" -> true
                else -> type.properties?.getString("type") == "sticky"
            }
            nukkitEntity["Sticky"] = sticky
        }
        149, 150 -> createTileEntity("Comparator")
        154 -> createTileEntity("Hopper") { nukkitEntity ->
            tileEntity?.apply {
                copyTo(nukkitEntity, "TransferCooldown")
                toNukkitInventory(nukkitEntity)
            }
        }
        84 -> createTileEntity("Jukebox") { nukkitEntity ->
            tileEntity?.getNullableCompound("RecordItem")?.let {
                nukkitEntity["RecordItem"] = it.toNukkitItem()
            }
        }
        205, 218 -> createTileEntity("ShulkerBox") { nukkitEntity ->
            val facing = when (type.properties?.getNullableString("facing")) {
                "down" -> 0
                "up" -> 1
                "north" -> 3
                "west" -> 4
                "east" -> 5
                else -> 0
            }
            nukkitEntity["facing"] = facing.toByte()
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                toNukkitInventory(nukkitEntity)
            }
        }
        176, 177 -> createTileEntity("Banner") { nukkitEntity ->
            val baseColor = when (type.blockName.removePrefix("minecraft:").removeSuffix("_banner").removeSuffix("_wall")) {
                "white" -> 0
                "orange" -> 1
                "magenta" -> 2
                "light_blue" -> 3
                "yellow" -> 4
                "lime" -> 5
                "pink" -> 6
                "gray" -> 7
                "light_gray" -> 8
                "cyan" -> 9
                "purple" -> 10
                "blue" -> 11
                "brown" -> 12
                "green" -> 13
                "red" -> 14
                "black" -> 15
                else -> 0
            }
            nukkitEntity["Base"] = baseColor
        }
        else -> tileEntity?.let { toNukkitTileEntity(it) }
    }

    return NukkitBlock(blockPos, blockData, nukkitTileEntity)
}

fun NbtCompound.entityToId(): Int? {
    val javaId = getNullableString("id")?.toLowerCase()?.removePrefix("minecraft:") ?: return null
    return java2bedrockEntities[javaId].takeUnless { it == 0 }
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

data class BlockData(var blockId: Int, var data: Int) {
    val byteBlockId: Byte get() = (blockId and 0xFF).toByte()
}
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

    return BlockData(blockId, blockData.toInt())
}
