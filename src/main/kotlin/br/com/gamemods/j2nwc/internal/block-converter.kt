package br.com.gamemods.j2nwc.internal

import br.com.gamemods.j2nwc.WorldConverter
import br.com.gamemods.nbtmanipulator.*
import br.com.gamemods.regionmanipulator.ChunkPos
import br.com.gamemods.regionmanipulator.Region
import net.md_5.bungee.api.ChatColor
import java.io.FileNotFoundException
import java.util.*
import kotlin.math.floor

private fun JavaBlock.commonBlockEntityData(id: String) = arrayOf(
    "id" to NbtString(id),
    "x" to NbtInt(blockPos.xPos),
    "y" to NbtInt(blockPos.yPos),
    "z" to NbtInt(blockPos.zPos),
    "isMoveable" to NbtByte(false)
)

private inline fun JavaBlock.createTileEntity(id: String, vararg tags: Pair<String, NbtTag>, action: (NbtCompound)->Unit = {}): NbtCompound {
    return NbtCompound(*commonBlockEntityData(id), *tags).also(action)
}

internal fun JavaBlock.toNukkit(
    regionPostConversionHooks: MutableList<PostConversionHook>,
    worldHooks: MutableList<PostWorldConversionHook>,
    worldConverter: WorldConverter
): NukkitBlock {
    val blockData = this.type.toNukkit()

    val nukkitTileEntity = when (blockData.blockId) {
        26 -> createTileEntity("Bed",
            "color" to NbtByte(when (type.blockName) {
                "minecraft:white_bed" -> 0
                "minecraft:orange_bed" -> 1
                "minecraft:magenta_bed" -> 2
                "minecraft:light_blue_bed" -> 3
                "minecraft:yellow_bed" -> 4
                "minecraft:lime_bed" -> 5
                "minecraft:pink_bed" -> 6
                "minecraft:gray_bed" -> 7
                "minecraft:light_gray_bed" -> 8
                "minecraft:cyan_bed" -> 9
                "minecraft:purple_bed" -> 10
                "minecraft:blue_bed" -> 11
                "minecraft:brown_bed" -> 12
                "minecraft:green_bed" -> 13
                "minecraft:red_bed" -> 14
                "minecraft:black_bed" -> 15
                else -> 14
            })
        )
        54 -> createTileEntity("Chest") { nukkitEntity ->
            tileEntity?.copyJsonToLegacyTo(nukkitEntity, "CustomName")
            val pair = when (type.properties?.getString("type")) {
                "left" -> when (type.properties?.getString("facing")) {
                    "east" -> blockPos.xPos to blockPos.zPos +1
                    "south" -> blockPos.xPos -1 to blockPos.zPos
                    "west" -> blockPos.xPos to blockPos.zPos -1
                    else -> blockPos.xPos +1 to blockPos.zPos
                }
                "right" -> when (type.properties?.getString("facing")) {
                    "east" -> blockPos.xPos to blockPos.zPos -1
                    "south" -> blockPos.xPos +1 to blockPos.zPos
                    "west" -> blockPos.xPos to blockPos.zPos +1
                    else -> blockPos.xPos -1 to blockPos.zPos
                }
                else -> null
            }
            tileEntity?.toNukkitInventory(nukkitEntity)
            pair?.also { (x, z) ->
                nukkitEntity["pairx"] = x
                nukkitEntity["pairz"] = z

                val y = blockPos.yPos
                val pairedChunkPos = ChunkPos(floor(x / 16.0).toInt(), floor(z / 16.0).toInt())
                val currentChunkPos = ChunkPos(floor(blockPos.xPos / 16.0).toInt(), floor(blockPos.zPos / 16.0).toInt())
                fun swapItems(nukkitRegion: Region) {
                    nukkitRegion[pairedChunkPos]?.level?.getCompoundList("TileEntities")
                        ?.find { it.getInt("x") == x && it.getInt("y") == y && it.getInt("z") == z }
                        ?.also { pairedEntity ->
                            if (pairedEntity.getNullableBooleanByte("--pair-processed--")) {
                                pairedEntity.remove("--pair-processed--")
                            } else {
                                val thisItems = nukkitEntity.getList("Items")
                                val otherItems = pairedEntity.getList("Items")

                                pairedEntity["Items"] = thisItems
                                nukkitEntity["Items"] = otherItems
                                nukkitEntity["--pair-processed--"] = true
                            }
                        }
                }

                val currentRegX = floor(currentChunkPos.xPos / 32.0).toInt()
                val currentRegZ = floor(currentChunkPos.zPos / 32.0).toInt()
                val pairedRegX = floor(pairedChunkPos.xPos / 32.0).toInt()
                val pairedRegZ = floor(pairedChunkPos.zPos / 32.0).toInt()
                if (currentRegX == pairedRegX && currentRegZ == pairedRegZ) {
                    regionPostConversionHooks += { _, nukkitRegion ->
                        swapItems(nukkitRegion)
                    }
                } else {
                    worldHooks += { _, worldDir ->
                        try {
                            modifyRegion(worldDir, pairedRegX, pairedRegZ, ::swapItems)
                        } catch (e: FileNotFoundException) {
                            System.err.println(
                                "Could not swap the double chest items between the chests $blockPos and ${BlockPos(
                                    x,
                                    y,
                                    z
                                )} because the file r.$pairedRegX.$pairedRegZ.mca does not exists!"
                            )
                            System.err.println(e.toString())
                        }
                    }
                }
            }
        }
        130 -> createTileEntity("EnderChest")
        61, 62 -> createTileEntity("Furnace") { nukkitEntity ->
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                copyTo(nukkitEntity, "BurnTime")
                copyTo(nukkitEntity, "CookTime")
                getNullableShort("CookTimeTotal")?.also {
                    nukkitEntity["BurnDuration"] = it
                }
                toNukkitInventory(nukkitEntity)
            }
        }
        117 -> createTileEntity("BrewingStand") { nukkitEntity ->
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                nukkitEntity["CookTime"] = getShort("BrewTime")
                val fuel = getNullableByte("Fuel") ?: 0
                val fuelTotal = if (fuel > 0) 20 else 0
                nukkitEntity["FuelTotal"] = fuelTotal.toShort()
                nukkitEntity["FuelAmount"] = fuel.toShort()
                toNukkitInventory(nukkitEntity) {
                    when {
                        it == 3 -> 0
                        it < 3 -> it + 1
                        else -> it
                    }
                }
            }
        }
        151, 178 -> createTileEntity("DaylightDetector")
        25 -> createTileEntity("Music") { nukkitEntity ->
            nukkitEntity["note"] = type.properties?.getString("note")?.toByte() ?: 0
            nukkitEntity["powered"] = type.properties?.getString("powered")?.toBoolean() ?: false
        }
        63,436,441,443,445,447,68,437,442,444,446,448,323,472,473,474,475,476 ->
            createTileEntity("Sign") { nukkitEntity ->
                val lines = StringBuilder()
                val color = when (tileEntity?.getNullableString("Color")) {
                    "white" -> ChatColor.WHITE
                    "orange" -> ChatColor.GOLD
                    "magenta" -> ChatColor.LIGHT_PURPLE
                    "light_blue" -> ChatColor.DARK_AQUA
                    "yellow" -> ChatColor.YELLOW
                    "lime" -> ChatColor.GREEN
                    "pink" -> ChatColor.LIGHT_PURPLE
                    "gray" -> ChatColor.DARK_GRAY
                    "light_gray" -> ChatColor.GRAY
                    "cyan" -> ChatColor.AQUA
                    "purple" -> ChatColor.DARK_PURPLE
                    "blue" -> ChatColor.DARK_BLUE
                    "brown" -> ChatColor.DARK_RED
                    "green" -> ChatColor.DARK_GREEN
                    "red" -> ChatColor.RED
                    "black" -> ChatColor.BLACK
                    else -> null
                }
                for (i in 1..4) {
                    val text = tileEntity?.getString("Text$i")?.fromJsonToLegacy() ?: ""
                    color?.let(lines::append)
                    lines.append(text).append('\n')
                }
                nukkitEntity["Text"] = lines.toString()
            }
        52 -> createTileEntity("MobSpawner") { nukkitEntity ->
            // Tile entity based on
            // https://github.com/Nukkit-coders/MobPlugin/blob/master/src/main/java/nukkitcoders/mobplugin/entities/block/BlockEntitySpawner.java
            tileEntity?.apply {
                copyTo(nukkitEntity, "SpawnRange")
                copyTo(nukkitEntity, "MinSpawnDelay")
                copyTo(nukkitEntity, "MaxSpawnDelay")
                copyTo(nukkitEntity, "MaxNearbyEntities")
                copyTo(nukkitEntity, "RequiredPlayerRange")
                nukkitEntity["EntityId"] = getNullableCompound("SpawnData")?.entityToId() ?: 12
            }
        }
        116 -> createTileEntity("EnchantTable") { nukkitEntity ->
            tileEntity?.copyJsonToLegacyTo(nukkitEntity, "CustomName")
        }
        144 -> createTileEntity("Skull") { nukkitEntity ->
            val rotation = type.properties?.getNullableString("rotation") ?: "0"
            val type = when (type.blockName.removePrefix("minecraft:")) {
                "skeleton_skull", "skeleton_wall_skull" -> 0
                "wither_skeleton_skull", "wither_skeleton_wall_skull" -> 1
                "zombie_head", "zombie_wall_head" -> 2
                "player_head", "player_wall_head" -> {
                    if (tileEntity?.containsKey("Owner") == true && worldConverter.skipSkinHeads) {
                        return NukkitBlock(blockPos, BlockData(0, 0), null)
                    } else {
                        3
                    }
                }
                "creeper_head", "creeper_wall_head" -> 4
                else -> 0
            }
            nukkitEntity["Rot"] = rotation.toByte()
            nukkitEntity["SkullType"] = type.toByte()
            if ("_wall_" !in this.type.blockName) {
                blockData.data = 1
            }
        }
        140 -> createTileEntity("FlowerPot") { nukkitEntity ->
            val potted = when (type.blockName.removePrefix("minecraft:")) {
                "potted_oak_sapling" -> BlockData(6, 0)
                "potted_spruce_sapling" -> BlockData(6, 1)
                "potted_birch_sapling" -> BlockData(6, 2)
                "potted_jungle_sapling" -> BlockData(6, 3)
                "potted_acacia_sapling" -> BlockData(6, 4)
                "potted_dark_oak_sapling" -> BlockData(6, 5)
                "potted_fern" -> BlockData(31, 1)
                "potted_dandelion" -> BlockData(37, 0)
                "potted_poppy" -> BlockData(38, 0)
                "potted_blue_orchid" -> BlockData(38, 1)
                "potted_allium" -> BlockData(38, 2)
                "potted_azure_bluet" -> BlockData(38, 3)
                "potted_red_tulip" -> BlockData(38, 4)
                "potted_orange_tulip" -> BlockData(38, 5)
                "potted_white_tulip" -> BlockData(38, 6)
                "potted_pink_tulip" -> BlockData(38, 7)
                "potted_oxeye_daisy" -> BlockData(38, 8)
                "potted_cornflower" -> BlockData(38, 9)
                "potted_lily_of_the_valley" -> BlockData(38, 10)
                "potted_wither_rose" -> BlockData(38, 2)
                "potted_brown_mushroom" -> BlockData(39, 0)
                "potted_red_mushroom" -> BlockData(40, 0)
                "potted_dead_bush" -> BlockData(32, 0)
                "potted_cactus" -> BlockData(81, 0)
                "potted_bamboo" -> BlockData(38, 0)
                else -> BlockData(0, 0)
            }
            potted.blockId.takeIf { it != 0 }?.let {
                nukkitEntity["item"] = it
            }

            potted.data.takeIf { it != 0 }?.let {
                nukkitEntity["data"] = it
            }
        }
        118 -> createTileEntity("Cauldron")
        138 -> createTileEntity("Beacon") { nukkitEntity ->
            tileEntity?.apply {
                val primary = javaStatusEffectNames[getNullableInt("Primary") ?: 0]?.let { java2bedrockEffectIds[it] }
                val secondary = javaStatusEffectNames[getNullableInt("Secondary") ?: 0]?.let { java2bedrockEffectIds[it] }
                primary?.let { nukkitEntity["Primary"] = it }
                secondary?.let { nukkitEntity["Secondary"] = it }
            }
        }
        33, 29 -> createTileEntity("PistonArm") { nukkitEntity ->
            val sticky = when (type.blockName) {
                "minecraft:piston" -> false
                "minecraft:sticky_piston" -> true
                else -> type.properties?.getString("type") == "sticky"
            }
            nukkitEntity["Sticky"] = sticky
        }
        149, 150 -> createTileEntity("Comparator")
        154 -> createTileEntity("Hopper") { nukkitEntity ->
            tileEntity?.apply {
                copyTo(nukkitEntity, "TransferCooldown")
                toNukkitInventory(nukkitEntity)
            }
        }
        84 -> createTileEntity("Jukebox") { nukkitEntity ->
            tileEntity?.getNullableCompound("RecordItem")?.let {
                nukkitEntity["RecordItem"] = it.toNukkitItem()
            }
        }
        205, 218 -> createTileEntity("ShulkerBox") { nukkitEntity ->
            val facing = when (type.properties?.getNullableString("facing")) {
                "down" -> 0
                "up" -> 1
                "north" -> 3
                "west" -> 4
                "east" -> 5
                else -> 0
            }
            nukkitEntity["facing"] = facing.toByte()
            tileEntity?.apply {
                copyJsonToLegacyTo(nukkitEntity, "CustomName")
                toNukkitInventory(nukkitEntity)
            }
        }
        176, 177 -> createTileEntity("Banner") { nukkitEntity ->
            val baseColor = when (type.blockName.removePrefix("minecraft:").removeSuffix("_banner").removeSuffix("_wall")) {
                "white" -> 15
                "orange" -> 14
                "magenta" -> 13
                "light_blue" -> 12
                "yellow" -> 11
                "lime" -> 10
                "pink" -> 9
                "gray" -> 8
                "light_gray" -> 7
                "cyan" -> 6
                "purple" -> 5
                "blue" -> 4
                "brown" -> 3
                "green" -> 2
                "red" -> 1
                "black" -> 0
                else -> 0
            }
            nukkitEntity["Base"] = baseColor
            tileEntity?.getNullableCompoundList("Patterns")?.also { patterns ->
                val nukkitPatterns = NbtList<NbtCompound>()
                patterns.forEach { pattern ->
                    val nukkitPattern = NbtCompound()
                    val patternCode = pattern.getNullableString("Pattern") ?: return@forEach
                    val patternColor = pattern.getNullableInt("Color") ?: return@forEach
                    nukkitPattern["Pattern"] = patternCode
                    nukkitPattern["Color"] = 15 - patternColor
                    nukkitPatterns += nukkitPattern
                }
                nukkitEntity["Patterns"] = nukkitPatterns
            }
        }
        else -> null
    }

    return NukkitBlock(blockPos, blockData, nukkitTileEntity)
}

internal data class BlockData(var blockId: Int, var data: Int) {
    val byteBlockId: Byte get() = (blockId and 0xFF).toByte()
}
internal fun JavaPalette.toNukkit(): BlockData {
    val propertiesId = properties
        ?.mapValuesTo(TreeMap()) { (it.value as NbtString).value }
        ?.map { "${it.key}-${it.value}" }
        ?.joinToString(";", prefix = ";")
        ?: ""

    val stateId = "$blockName$propertiesId".removePrefix("minecraft:").replace(':', '-').toLowerCase()

    val bedrockState = java2bedrockStates[stateId]
    if (bedrockState == null) {
        System.err.println("Missing block state mapping for $stateId")
    }
    val prop = bedrockState ?: java2bedrockStates[blockName] ?: "1,15"
    val nukkit = bedrock2nukkit.getProperty("B,$prop") ?: prop
    val ids = nukkit.split(',', limit = 2)
    val blockId = ids[0].toInt()
    val blockData = ids.getOrElse(1) { "0" }.toByte()
    check(blockId in 0..255) {
        "Block id unsupported by Nukkit: $blockId:$blockData"
    }

    return BlockData(blockId, blockData.toInt())
}
