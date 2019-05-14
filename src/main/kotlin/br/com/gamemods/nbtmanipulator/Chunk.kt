package br.com.gamemods.nbtmanipulator

import java.util.*

data class Chunk(var lastModified: Date, var nbtFile: NbtFile) {
    val compound: NbtCompound
        get() = nbtFile.compound

    val dataVersion: Int
        get() = compound.getInt("DataVersion")

    val level: NbtCompound
        get() = compound.getCompound("Level")

    val position: ChunkPos
        get() = level.let {
            ChunkPos(it.getInt("xPos"), it.getInt("zPos"))
        }
}