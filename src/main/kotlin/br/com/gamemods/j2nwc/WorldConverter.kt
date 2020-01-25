package br.com.gamemods.j2nwc

import br.com.gamemods.j2nwc.internal.PostWorldConversionHook
import br.com.gamemods.j2nwc.internal.convertLevelFile
import br.com.gamemods.j2nwc.internal.convertRegionFile
import java.io.File
import java.io.IOException

/**
 * A region position extracted from the region file name.
 *
 * `r.-2.3.mca` must be `Region(-2,3)` for example
 * @property xPos The first number in the region file name. May be negative.
 * @property zPos The second number in the region file name. May be negative.
 */
@Deprecated(
    "Already provided by Region-Manipulator",
    ReplaceWith("RegionPosition", "br.com.gamemods.j2nwc.RegionPosition")
)
data class RegionPos
    @Deprecated(
        "Already provided by Region-Manipulator",
        ReplaceWith("RegionPosition(xPos, zPos)", "br.com.gamemods.j2nwc.RegionPosition")
    )
    constructor (val xPos: Int, val zPos: Int) {

    @Suppress("DEPRECATION")
    private constructor(mcaFileNameParts: List<String>): this(mcaFileNameParts[1].toInt(), mcaFileNameParts[2].toInt())

    /**
     * Parses a region file name. Only support valid names like `r.-3.2.mca`.
     * @param mcaFileName A valid file name
     */
    @Deprecated(
        "Already provided by Region-Manipulator",
        ReplaceWith("RegionPosition(mcaFileName)", "br.com.gamemods.j2nwc.RegionPosition")
    )
    constructor(mcaFileName: String): this(mcaFileName.split('.'))

    /**
     * Converts this deprecated object to it's replacement.
     */
    fun toRegionManipulator() = RegionPosition(xPos, zPos)
}

/**
 * A region position extracted from the region file name.
 *
 * `r.-2.3.mca` must be `Region(-2,3)` for example
 */
typealias RegionPosition = br.com.gamemods.regionmanipulator.RegionPos

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
    @Suppress("DEPRECATION")
    @Deprecated(
        "Uses a duplicated type, please use regions instead",
        ReplaceWith("regions")
    )
    var regionFilter = mutableSetOf<RegionPos>()

    /**
     * A collection which determines which region files will be converted. When empty all regions are converted.
     */
    var regions = mutableSetOf<RegionPosition>()

    /**
     * Determines if player heads which contains custom skins should be skipped.
     *
     * Player heads without skins are unaffected.
     */
    var skipSkinHeads = true

    /**
     * Executes the conversion in the current thread. Will take a while to complete.
     *
     * @throws IOException If an error occurs while loading or writing the files
     */
    @Throws(IOException::class)
    fun convert() {
        check(to.isDirectory || to.mkdirs()) {
            "Failed to create the folder $to"
        }

        convertLevelFile(File(from, "level.dat"), File(to, "level.dat"), this)

        val toRegionDir = File(to, "region")
        toRegionDir.mkdirs()
        val worldHooks = mutableListOf<PostWorldConversionHook>()
        @Suppress("DEPRECATION")
        val regions = (regions + regionFilter.map { it.toRegionManipulator() }).toSet()
        File(from, "region").listFiles().asSequence()
            .filter { it.name.toLowerCase().matches(Regex("""^r\.-?\d+\.-?\d+\.mca$""")) }
            .filter { regions.isEmpty() || RegionPosition(it.name) in regions }
            .forEach { fromRegion ->
                convertRegionFile(
                    fromRegion,
                    File(toRegionDir, fromRegion.name),
                    worldHooks,
                    this
                )
            }
        worldHooks.forEach {
            it(from, to)
        }
    }
}
