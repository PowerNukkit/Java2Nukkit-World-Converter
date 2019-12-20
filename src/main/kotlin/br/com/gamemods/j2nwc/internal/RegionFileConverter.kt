@file:JvmName("RegionFileConverter")
package br.com.gamemods.j2nwc.internal

import br.com.gamemods.j2nwc.WorldConverter
import br.com.gamemods.regionmanipulator.Chunk
import br.com.gamemods.regionmanipulator.Region
import br.com.gamemods.regionmanipulator.RegionIO
import java.io.File

internal typealias PostWorldConversionHook = (from: File, to: File) -> Unit

internal fun convertRegionFile(
    from: File,
    to: File,
    worldHooks: MutableList<PostWorldConversionHook>,
    worldConverter: WorldConverter
) {
    val javaRegion = RegionIO.readRegion(from)
    val nukkitRegion = javaRegion.toNukkit(worldHooks, worldConverter)
    RegionIO.writeRegion(to, nukkitRegion)
}

internal inline fun modifyRegion(worldDir: File, xPos: Int, zPos: Int, modify: (Region) -> Unit) {
    val regionFile = File(worldDir, "region/r.$xPos.$zPos.mca")
    RegionIO.readRegion(regionFile).let {
        modify(it)
        RegionIO.writeRegion(regionFile, it)
    }
}

internal typealias PostConversionHook = (javaRegion: Region, nukkitRegion: Region) -> Unit

internal fun Region.toNukkit(
    worldHooks: MutableList<PostWorldConversionHook>,
    worldConverter: WorldConverter
): Region {
    val postConversionHooks = mutableListOf<PostConversionHook>()
    val nukkitRegion = Region(position, values.mapNotNull {
        it.toNukkit(postConversionHooks, worldHooks, worldConverter)?.let { nukkitChunk ->
            Chunk(it.lastModified, nukkitChunk.toNbt())
        }
    })
    postConversionHooks.forEach {
        it(this, nukkitRegion)
    }
    return nukkitRegion
}
