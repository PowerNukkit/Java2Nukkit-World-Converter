package br.com.gamemods.nbtmanipulator

import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

fun Region.toNukkit(): Region {
    return Region(position, values.map { Chunk(it.lastModified, it.toNukkit().toNbt()) })
}

fun Chunk.toNukkit(): NukkitChunk {
    val javaChunk = JavaChunk(this)
    val nukkitSections = javaChunk.sections.entries.asSequence()
        .filter { it.value.yPos >= 0 }
        .mapNotNull { it.key to (it.value.toNukkit() ?: return@mapNotNull null) }
        .toMap()
    val nukkitChunk = NukkitChunk(
        NbtList(javaChunk.entities.value.map { toNukkitEntity(it) }),
        nukkitSections,
        NbtList(javaChunk.tileEntities.value.map { toNukkitTileEntity(it) }),
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

fun JavaChunkSection.toNukkit(): NukkitChunkSection? {
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
        palette[paletteIndexes[it]]
    }

    val nukkitBlocks = blockPalettes.map { it.toNukkit() }

    return NukkitChunkSection(
        yPos = yPos,
        blockLight = ByteArray(2048),
        blocks = ByteArray(4096) { nukkitBlocks[it].blockId },
        blockData = ByteArray(2048) {
            val double = it * 2
            val stored = nukkitBlocks[double].data to nukkitBlocks[double + 1].data
            val first = stored.first.toInt() and 0x0F
            val second = (stored.second.toInt() and 0x0F) shl 4
            val merged = first or second
            (merged and 0xFF).toByte()
            /*(((nukkitBlocks[double].data.toInt() shl 4) and 0xF0) or
                    (nukkitBlocks[double + 1].data.toInt() and 0x0F)
            ).toByte()*/
        },
        skyLight = skyLight ?: ByteArray(2048)
    )
}

fun toNukkitTileEntity(javaEntity: NbtCompound): NbtCompound {
    return javaEntity
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
