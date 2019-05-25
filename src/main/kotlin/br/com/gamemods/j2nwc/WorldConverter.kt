package br.com.gamemods.j2nwc

import java.io.File

/**
 * A region position extracted from the region file name.
 *
 * `r.-2.3.mca` must be `Region(-2,3)` for example
 * @property xPos The first number in the region file name. May be negative.
 * @property zPos The second number in the region file name. May be negative.
 */
data class RegionPos(val xPos: Int, val zPos: Int) {
    private constructor(mcaFileNameParts: List<String>): this(mcaFileNameParts[1].toInt(), mcaFileNameParts[2].toInt())

    /**
     * Parses a region file name. Only support valid names like `r.-3.2.mca`.
     * @param mcaFileName A valid file name
     */
    constructor(mcaFileName: String): this(mcaFileName.split('.'))
}

/**
 * Prepare a conversion from a Java World 1.14+ to Nukkit.
 *
 * Please check [this page](https://gamemodsbr.github.io/Java2Nukkit-World-Converter/) for details about the requirements and expectations.
 * @property from The Java world folder
 * @property to The world folder that will be created for the Nukkit's world
 */
class WorldConverter(val from: File, val to: File) {
    /**
     * A collection which determines which region files will be converted. When empty all regions are converted.
     */
    var regionFilter = mutableSetOf<RegionPos>()

    /**
     * Executes the conversion in the current thread. Will take a while to complete.
     */
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
