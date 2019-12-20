package br.com.gamemods.j2nwc.internal

import br.com.gamemods.j2nwc.WorldConverter
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.regionmanipulator.Chunk

internal fun Chunk.toNukkit(
    regionPostConversionHooks: MutableList<PostConversionHook>,
    worldHooks: MutableList<PostWorldConversionHook>,
    worldConverter: WorldConverter
): NukkitChunk? {
    val javaChunk = JavaChunk(this)
    if (javaChunk.sections.isEmpty()) {
        if (javaChunk.status == "structure_starts") {
            return null
        } /*else {
            println("Empty Chunk: ${javaChunk.position} ${javaChunk.status}")
        }*/
    } /*else if (javaChunk.status != "full") {
        println("Chunk status: ${javaChunk.status}")
    }*/
    val javaTileEntities = javaChunk.tileEntities.associate {
        BlockPos(it.getInt("x"), it.getInt("y"), it.getInt("z")) to it
    }
    val nukkitTileEntities = mutableMapOf<BlockPos, NbtCompound>()
    val nukkitSections = javaChunk.sections.entries.asSequence()
        .filter { it.value.yPos >= 0 }
        .mapNotNull {
            it.key to (
                it.value.toNukkit(javaTileEntities, nukkitTileEntities, regionPostConversionHooks, worldHooks, worldConverter)
                    ?: return@mapNotNull null
            )
        }
        .toMap()
    val nukkitChunk = NukkitChunk(
        NbtList(javaChunk.entities.mapNotNull {
            toNukkitEntity(
                it,
                javaChunk,
                nukkitSections,
                nukkitTileEntities,
                regionPostConversionHooks,
                worldHooks,
                worldConverter
            )
        }),
        nukkitSections,
        NbtList(nukkitTileEntities.values.toMutableList()),
        javaChunk.inhabitedTime,
        false,
        javaChunk.status != "empty",
        javaChunk.status != "empty",
        1,
        javaChunk.position,
        javaChunk.toNukkitBiomes(),
        byteArrayOf(0, 0, 0, 0),
        IntArray(256) { 255 }
    )
    return nukkitChunk
}

internal data class BlockPos(val xPos: Int, val yPos: Int, val zPos: Int)
internal data class JavaBlock(val blockPos: BlockPos, val type: JavaPalette, var tileEntity: NbtCompound?)
internal data class NukkitBlock(val blockPos: BlockPos, var blockData: BlockData, var tileEntity: NbtCompound?)
internal fun JavaChunkSection.toNukkit(
    javaTileEntities: Map<BlockPos, NbtCompound>,
    nukkitTileEntities: MutableMap<BlockPos, NbtCompound>,
    regionPostConversionHooks: MutableList<PostConversionHook>,
    worldHooks: MutableList<PostWorldConversionHook>,
    worldConverter: WorldConverter
): NukkitChunkSection? {
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
        val blockPos =
            BlockPos(x + chunkPos.xPos * 16, y + yPos * 16, z + chunkPos.zPos * 16)
        JavaBlock(blockPos, palette[paletteIndexes[it]], javaTileEntities[blockPos])
    }

    val nukkitBlocks = blockPalettes.map { it.toNukkit(regionPostConversionHooks, worldHooks, worldConverter).also { block ->
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

internal fun JavaChunk.toNukkitBiomes(): ByteArray {
    val biomes = this.biomes ?: emptyList()
    //TODO Get the biome from the highest block in a given XZ coordinate inside the chunk
    //val heightMap = heightMap?.getLongArray("WORLD_SURFACE") ?: LongArray(256) { 64 }
    val biomes256 = IntArray(256) { index ->
        val z = index and 0xF
        val x = (index shr 4) and 0xF
        val y = 64
        val javaBiome = biomes
            .firstOrNull { x in it.x..it.lastX && z in it.z..it.lastZ && y in it.y..it.lastY }
            ?: biomes.firstOrNull { x in it.x..it.lastX && z in it.z..it.lastZ }

        javaBiome?.biome ?: 1
    }

    return biomes256.map { id ->
        val remap = javaBiomes2Bedrock[id]
        if (remap == null) {
            System.err.println("Unmapped biome with id $id")
        }
        remap ?: 1
    }.toByteArray()
}
