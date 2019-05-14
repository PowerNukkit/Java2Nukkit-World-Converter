package br.com.gamemods.nbtmanipulator

data class NukkitChunkSection(
    var yPos: Int,
    var blockLight: ByteArray,
    var blocks: ByteArray,
    var blockData: ByteArray,
    var skyLight: ByteArray
) {
    fun toNbt(): NbtCompound {
        return NbtCompound(
            "Y" to NbtByte(yPos.toByte()),
            "BlockLight" to NbtByteArray(blockLight),
            "Blocks" to NbtByteArray(blocks),
            "Data" to NbtByteArray(blockData),
            "SkyLight" to NbtByteArray(skyLight)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NukkitChunkSection

        if (yPos != other.yPos) return false
        if (!blockLight.contentEquals(other.blockLight)) return false
        if (!blocks.contentEquals(other.blocks)) return false
        if (!blockData.contentEquals(other.blockData)) return false
        if (!skyLight.contentEquals(other.skyLight)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yPos
        result = 31 * result + blockLight.contentHashCode()
        result = 31 * result + blocks.contentHashCode()
        result = 31 * result + blockData.contentHashCode()
        result = 31 * result + skyLight.contentHashCode()
        return result
    }
}

data class NukkitChunk(
    var entities: NbtList<NbtCompound>,
    var sections: Map<Int, NukkitChunkSection>,
    var tileEntities: NbtList<NbtCompound>,
    var inhabitedTime: Long,
    var lightPopulated: Boolean,
    var terrainGenerated: Boolean,
    var terrainPopulated: Boolean,
    var v: Byte,
    var position: ChunkPos,
    var biomes: ByteArray,
    var extraData: ByteArray,
    var heightMap: IntArray
) {
    fun toNbt(): NbtFile {
        val level = NbtCompound()
        level["Entities"] = entities
        level["Sections"] = NbtList(sections.values.map { it.toNbt() })
        level["TileEntities"] = tileEntities
        level["InhabitedTime"] = NbtLong(inhabitedTime)
        level["LightPopulated"] = NbtByte(lightPopulated)
        level["TerrainPopulated"] = NbtByte(terrainPopulated)
        level["V"] = NbtByte(v)
        level["xPos"] = NbtInt(position.xPos)
        level["zPos"] = NbtInt(position.zPos)
        level["Biomes"] = NbtByteArray(biomes)
        level["ExtraData"] = NbtByteArray(extraData)
        level["HeightMap"] = NbtIntArray(heightMap)

        return NbtFile("", NbtCompound("Level" to level))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NukkitChunk

        if (entities != other.entities) return false
        if (sections != other.sections) return false
        if (tileEntities != other.tileEntities) return false
        if (inhabitedTime != other.inhabitedTime) return false
        if (lightPopulated != other.lightPopulated) return false
        if (terrainGenerated != other.terrainGenerated) return false
        if (terrainPopulated != other.terrainPopulated) return false
        if (v != other.v) return false
        if (position != other.position) return false
        if (!biomes.contentEquals(other.biomes)) return false
        if (!extraData.contentEquals(other.extraData)) return false
        if (!heightMap.contentEquals(other.heightMap)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entities.hashCode()
        result = 31 * result + sections.hashCode()
        result = 31 * result + tileEntities.hashCode()
        result = 31 * result + inhabitedTime.hashCode()
        result = 31 * result + lightPopulated.hashCode()
        result = 31 * result + terrainGenerated.hashCode()
        result = 31 * result + terrainPopulated.hashCode()
        result = 31 * result + v
        result = 31 * result + position.hashCode()
        result = 31 * result + biomes.contentHashCode()
        result = 31 * result + extraData.contentHashCode()
        result = 31 * result + heightMap.contentHashCode()
        return result
    }
}