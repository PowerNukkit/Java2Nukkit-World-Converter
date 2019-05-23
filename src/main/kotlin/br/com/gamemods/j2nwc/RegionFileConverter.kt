@file:JvmName("RegionFileConverter")
package br.com.gamemods.j2nwc

import java.io.File

internal fun convertRegionFile(from: File, to: File) {
    val javaRegion = RegionIO.readRegion(from)
    val nukkitRegion = javaRegion.toNukkit()
    RegionIO.writeRegion(to, nukkitRegion)
    val test = RegionIO.readRegion(to)
    println(test.hashCode())
}
