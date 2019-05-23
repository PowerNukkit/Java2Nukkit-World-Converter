@file:JvmName("WorldConverter")
package br.com.gamemods.j2nwc

import java.io.File

class WorldConverter(val from: File, val to: File) {
    fun convert() {
        check(to.isDirectory || to.mkdirs()) {
            "Falha ao criar a pasta $to"
        }

        convertLevelFile(File(from, "level.dat"), File(to, "level.dat"))

        val toRegionDir = File(to, "region")
        toRegionDir.mkdirs()
        File(from, "region").listFiles().forEach { fromRegion ->
            if (fromRegion.name.toLowerCase().matches(Regex("""^r\.\d\.\d\.mca$"""))) {
                convertRegionFile(fromRegion, File(toRegionDir, fromRegion.name))
                return
            }
        }
    }
}