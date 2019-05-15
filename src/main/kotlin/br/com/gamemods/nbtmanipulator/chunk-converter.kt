package br.com.gamemods.nbtmanipulator

import java.util.*

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

fun JavaBlock.toNukkit(javaBlocks: Map<BlockPos, JavaBlock>): NukkitBlock {
    val blockData = this.type.toNukkit()

    if (type.blockName == "minecraft:chest") {
        println("Chest!")
    }

    fun commonBlockEntityData(id: String) = arrayOf(
        "id" to NbtString(id),
        "x" to NbtInt(blockPos.xPos),
        "y" to NbtInt(blockPos.yPos),
        "z" to NbtInt(blockPos.zPos)
    )

    val nukkitTileEntity = when (blockData.blockId.toInt()) {
        // bed
        26 -> NbtCompound(
            *commonBlockEntityData("Bed"),
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
        // chest
        54 -> NbtCompound(*commonBlockEntityData("Chest")).also { nukkitEntity ->
            //TODO Convert items from the chest
            tileEntity?.copy(nukkitEntity, "CustomName")
        }
        else -> tileEntity?.let { toNukkitTileEntity(it) }
    }

    return NukkitBlock(blockPos, blockData, nukkitTileEntity)
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
    val nukkit = bedrock2nukkit.getProperty(prop) ?: prop
    val ids = nukkit.split(',', limit = 2)
    val blockId = ids[0].toInt()
    check(blockId in 0..255) {
        "Block id unsupported by Nukkit: $blockId"
    }

    return BlockData((blockId and 0xFF).toByte(), ids.getOrElse(1) { "0" }.toByte())
}
