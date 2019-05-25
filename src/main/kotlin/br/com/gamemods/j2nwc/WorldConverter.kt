package br.com.gamemods.j2nwc

import java.io.File

typealias PostWorldConversionHook = (from: File, to: File) -> Unit
class WorldConverter(val from: File, val to: File) {
    fun convert() {
        check(to.isDirectory || to.mkdirs()) {
            "Falha ao criar a pasta $to"
        }

        convertLevelFile(File(from, "level.dat"), File(to, "level.dat"))

        val toRegionDir = File(to, "region")
        toRegionDir.mkdirs()
        val worldHooks = mutableListOf<PostWorldConversionHook>()
        File(from, "region").listFiles().filter { it.name in listOf("r.0.0.mca", "r.0.-1.mca") }.forEach { fromRegion ->
            if (fromRegion.name.toLowerCase().matches(Regex("""^r\.-?\d\.-?\d\.mca$"""))) {
                convertRegionFile(fromRegion, File(toRegionDir, fromRegion.name), worldHooks)
            }
        }
        worldHooks.forEach {
            it(from, to)
        }
    }
}