@file:JvmName("RegionFileConverter")
package br.com.gamemods.j2nwc

import br.com.gamemods.regionmanipulator.Chunk
import br.com.gamemods.regionmanipulator.Region
import br.com.gamemods.regionmanipulator.RegionIO
import java.io.File

fun convertRegionFile(from: File, to: File) {
    val javaRegion = RegionIO.readRegion(from)
    val nukkitRegion = javaRegion.toNukkit()
    RegionIO.writeRegion(to, nukkitRegion)
    val test = RegionIO.readRegion(to)
    println(test.hashCode())
}

fun Region.toNukkit(): Region {
    return Region(position, values.map { Chunk(it.lastModified, it.toNukkit().toNbt()) })
}
