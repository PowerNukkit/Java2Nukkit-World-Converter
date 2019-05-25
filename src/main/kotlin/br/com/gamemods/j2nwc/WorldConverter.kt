package br.com.gamemods.j2nwc

import java.io.File

typealias PostWorldConversionHook = (from: File, to: File) -> Unit
data class RegionPos(val xPos: Int, val zPos: Int) {
    private constructor(mcaFileNameParts: List<String>): this(mcaFileNameParts[1].toInt(), mcaFileNameParts[2].toInt())
    constructor(mcaFileName: String): this(mcaFileName.split('.'))
}
class WorldConverter(val from: File, val to: File) {
    var regionFilter = mutableSetOf<RegionPos>()
    fun convert() {
        check(to.isDirectory || to.mkdirs()) {
            "Failed to create the folder $to"
        }

        convertLevelFile(File(from, "level.dat"), File(to, "level.dat"))

        val toRegionDir = File(to, "region")
        toRegionDir.mkdirs()
        val worldHooks = mutableListOf<PostWorldConversionHook>()
        File(from, "region").listFiles().asSequence()
            .filter { it.name.toLowerCase().matches(Regex("""^r\.-?\d\.-?\d\.mca$""")) }
            .filter { regionFilter.isEmpty() || RegionPos(it.name) in regionFilter }
            .forEach { fromRegion ->
                convertRegionFile(fromRegion, File(toRegionDir, fromRegion.name), worldHooks)
            }
        worldHooks.forEach {
            it(from, to)
        }
    }
}
