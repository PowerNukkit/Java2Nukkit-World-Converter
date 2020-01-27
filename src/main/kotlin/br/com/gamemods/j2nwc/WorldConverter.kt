package br.com.gamemods.j2nwc

import br.com.gamemods.j2nwc.internal.*
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
     * The level format which will be used by the converted world.
     */
    var targetType = TargetType.NUKKIT

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
        checkNotNull(File(from, "region").listFiles(), { "$from is not a directory" }).asSequence()
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

    /**
     * The targeted save format that will be written in the output.
     */
    enum class TargetType(
        val maxBlockId: Int,
        val maxDataValue: Int,
        conversionMappings: String,
        blockIdsFile: String,
        itemIdsFile: String,
        val suprpessWarningItemIds: Set<Int> = emptySet()
    ) {
        /**
         * Supports Nukkit 1.X. Has Block ID limited to 255, blocks with higher ID will be removed or remapped to a similar block.
         *
         * PowerNukkit 1.X accepts this format normally.
         *
         * Nukkit 2.X and PowerNukkit 2.X accepts this format but will converto to LevelDB on load.
         */
        NUKKIT(255, 15, "/bedrock-2-nukkit.properties", "/nukkit-block-ids.properties", "/nukkit-item-ids.properties",
            setOf(434, 736, 737)),

        /**
         * Supports PowerNukkit 1.X. Doesn't have the Block ID limitation and almost all blocks will match the original world.
         *
         * Nukkit 1.X will reset chunks which contains waterlogged blocks and blocks with ID > 255 and log them as corrupt chunks.
         *
         * The behaviour of Nukkit 2.X with this format is unknown.
         *
         * PowerNukkit 2.X accepts this format but will convert to LevelDB on load.
         */
        POWER_NUKKIT(512, 63, "/bedrock-2-powernukkit.properties", "/powernukkit-block-ids.properties", "/powernukkit-item-ids.properties",
            setOf(434)),

        /**
         * Supports Nukkit 2.X. Doesn't have Block ID limitation but many blocks will be removed or remapped to a
         * similar block because it's not implemented by NukkitX yet.
         *
         * Nukkit 1.X and PowerNukkit 1.X won't load from this save format.
         *
         * PowerNukkit 2.X accepts this format normally.
         */
        //TODO #86 NUKKIT_V2,

        /**
         * Supports PowerNukkit 2.x. Doesn't have the Block ID limitation and almost all blocks will match the original world.
         *
         * Nukkit 1.X and PowerNukkit 1.X won't load from this save format.
         *
         * Nukkit 2.X accepts this format but unsupported blocks might be interpreted as custom or removed, the exact behaviour is unknown.
         */
        //TODO #86 POWER_NUKKIT_V2
        ;
        internal val bedrock2target by lazy { properties(conversionMappings) }
        internal val blockIds by lazy { propertiesStringInt(blockIdsFile) }
        internal val itemIds by lazy { propertiesStringInt(itemIdsFile) }

        internal val blockNames by lazy {
            blockIds.entries.asSequence()
                .map { (k, v) -> k to v }.groupBy { (_, v) -> v }
                .mapValues { it.value.map { p -> p.first } }
        }

        internal val itemNames by lazy {
            itemIds.entries.asSequence()
                .map { (k, v) -> k to v }.groupBy { (_, v) -> v }
                .mapValues { it.value.map { p -> p.first } }
                .let {
                    mapOf(0 to "air") + it
                }
        }

        internal val javaTags2Target by lazy {
            javaTags.mapValues { entry ->
                entry.value.asSequence().flatMap { javaBlock ->
                    val (bedrockId, bedrockData) = java2bedrockStates[javaBlock]?.split(',', limit = 2) ?: listOf("0","0").also {
                        //println("The tag ${entry.key} points to a missing block $javaBlock")
                    }
                    val (nukkitId, _) = (
                            bedrock2target.getProperty("B,$bedrockId,$bedrockData") ?: "$bedrockId,$bedrockData"
                            ).split(',', limit = 2)
                    if (nukkitId != "0") {
                        blockNames[nukkitId.toInt()]?.asSequence() ?: sequenceOf(nukkitId, javaBlock)
                    } else {
                        sequenceOf(null)
                    }
                }.filterNotNull().toList()
            }
        }

        internal val javaBlockProps2Target by lazy {
            javaTags2Target + java2bedrockStates.asSequence().filter { ';' !in it.key }.flatMap { (javaBlock, mapping) ->
                val (bedrockId, bedrockData) = mapping.split(',', limit = 2)
                val (nukkitId, _) = (
                        bedrock2target.getProperty("B,$bedrockId,$bedrockData") ?: "$bedrockId,$bedrockData"
                        ).split(',', limit = 2)
                if (nukkitId != "0") {
                    sequenceOf(javaBlock to (blockNames[nukkitId.toInt()] ?: listOf(nukkitId, javaBlock)))
                } else {
                    sequenceOf(null)
                }
            }.filterNotNull()
        }
    }
}
