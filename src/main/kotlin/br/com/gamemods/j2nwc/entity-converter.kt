package br.com.gamemods.j2nwc

import br.com.gamemods.nbtmanipulator.*
import br.com.gamemods.regionmanipulator.ChunkPos
import br.com.gamemods.regionmanipulator.Region
import java.io.FileNotFoundException
import kotlin.math.floor

internal fun toNukkitEntity(
    javaEntity: NbtCompound,
    javaChunk: JavaChunk,
    nukkitSections: Map<Int, NukkitChunkSection>,
    nukkitTileEntities: MutableMap<BlockPos, NbtCompound>,
    regionPostConversionHooks: MutableList<PostConversionHook>,
    worldHooks: MutableList<PostWorldConversionHook>
): NbtCompound? {
    fun convertBaseEntity(): NbtCompound? {
        return NbtCompound().apply {
            val nukkitId = javaEntities2Nukkit[javaEntity.getNullableString("id")?.removePrefix("minecraft:") ?: return null] ?: return null
            this["id"] = nukkitId
            copyFrom(javaEntity, "CustomName")
            copyFrom(javaEntity, "CustomNameVisible")
            copyFrom(javaEntity, "Pos")
            copyFrom(javaEntity, "Motion")
            copyFrom(javaEntity, "Rotation")
            copyFrom(javaEntity, "FallDistance")
            copyFrom(javaEntity, "Fire")
            copyFrom(javaEntity, "Air")
            copyFrom(javaEntity, "OnGround")
            copyFrom(javaEntity, "Invulnerable")
            copyFrom(javaEntity, "Scale")
            copyFrom(javaEntity, "Silent")
            copyFrom(javaEntity, "NoGravity")
            copyFrom(javaEntity, "Glowing")
            //TODO Scoreboard Tags
            //TODO Passengers
        }
    }
    return when(javaEntity.getString("id").removePrefix("minecraft:")) {
        "falling_block" -> {
            val nukkitEntity = convertBaseEntity() ?: return null
            val javaTileEntity = javaEntity.getNullableCompound("TileEntityData")
            val javaBlockState = javaEntity.getNullableCompound("BlockState")
                ?: NbtCompound("Name" to NbtString("minecraft:sand"))
            val javaPalette = JavaPalette(
                javaBlockState.getNullableString("Name") ?: "minecraft:sand",
                javaBlockState.getNullableCompound("Properties")
            )
            val javaBlock = JavaBlock(BlockPos(0, 0, 0), javaPalette, javaTileEntity)
            val nukkitBlock = javaBlock.toNukkit(mutableListOf(), mutableListOf())
            if (nukkitBlock.blockData.blockId == 0) {
                null
            } else {
                nukkitEntity["TileID"] = nukkitBlock.blockData.blockId
                nukkitEntity["Data"] = nukkitBlock.blockData.data.toByte()
                nukkitEntity
            }
        }
        "painting" -> {
            val nukkitEntity = convertBaseEntity() ?: return null
            val paintingData = paintings[javaEntity.getNullableString("Motive")?.removePrefix("minecraft:") ?: ""] ?: return null
            nukkitEntity["Motive"] = paintingData.id
            val javaFacing = javaEntity.getNullableByte("Facing")
            val (nukkitDirection, xInc, zInc) = when (javaFacing?.toInt()) {
                0 -> arrayOf(3, 0, 1) // south
                1 -> arrayOf(4, -1, 0) // west
                2 -> arrayOf(2, 0, -1) // north
                3 -> arrayOf(5, 1, 0) // east
                else -> arrayOf(2, 0, -1) // north
            }

            val javaTileX = javaEntity.getInt("TileX")
            val tileY = javaEntity.getInt("TileY")
            val javaTileZ = javaEntity.getInt("TileZ")

            val nukkitTileX = javaTileX + xInc*-1
            val nukkitTileZ = javaTileZ + zInc*-1
            nukkitEntity["Pos"] = NbtList(
                NbtDouble(nukkitTileX.toDouble()),
                NbtDouble(tileY.toDouble()),
                NbtDouble(nukkitTileZ.toDouble())
            )
            nukkitEntity["TileX"] = nukkitTileX
            nukkitEntity["TileY"] = tileY
            nukkitEntity["TileZ"] = nukkitTileZ
            nukkitEntity["Direction"] = nukkitDirection.toByte()
            nukkitEntity["Rotation"] = NbtList(NbtFloat(nukkitDirection * 90F), NbtFloat(0F))

            val nukkitChunkPos = ChunkPos(floor(nukkitTileX / 16.0).toInt(), floor(nukkitTileZ / 16.0).toInt())
            val javaChunkPos = ChunkPos(floor(javaTileX / 16.0).toInt(), floor(javaTileZ / 16.0).toInt())
            if (javaChunk.position == nukkitChunkPos) {
                nukkitEntity
            } else {
                val nukkitRegX = floor(nukkitChunkPos.xPos / 32.0).toInt()
                val nukkitRegZ = floor(nukkitChunkPos.zPos / 32.0).toInt()
                val javaRegX = floor(javaChunkPos.xPos / 32.0).toInt()
                val javaRegZ = floor(javaChunkPos.zPos / 32.0).toInt()
                fun addEntity(nukkitRegion: Region) {
                    nukkitRegion[nukkitChunkPos]?.level?.getCompoundList("Entities")?.value?.add(nukkitEntity)
                }
                if (nukkitRegX == javaRegX && nukkitRegZ == javaRegZ) {
                    regionPostConversionHooks += { _, nukkitRegion ->
                        addEntity(nukkitRegion)
                    }
                } else {
                    worldHooks += { _, worldDir ->
                        try {
                            modifyRegion(worldDir, nukkitRegX, nukkitRegZ, ::addEntity)
                        } catch (e: FileNotFoundException) {
                            System.err.println("Unable to migrate the painting at $javaTileX,$tileY,$javaTileZ because " +
                                    "the region file r.$nukkitRegX,$nukkitRegZ.mca does not exists")
                        }
                    }
                }
                null
            }
        }
        "item_frame" -> {
            val tileX = javaEntity.getInt("TileX")
            val tileY = javaEntity.getInt("TileY")
            val tileZ = javaEntity.getInt("TileZ")
            val chunkSectionY = tileY / 16
            val chunkSection = nukkitSections[chunkSectionY] ?: return null
            val internalX = tileX % 32
            val internalY = tileY % 32
            val internalZ = tileZ % 32
            val offset = ((internalY and 0xF) shl 8) + ((internalZ and 0xF) shl 4) + (internalX and 0xF)
            val blockIdInPosition = chunkSection.blocks[offset].toInt()
            // Item Frame in Bedrock Edition is a block and not an entity, check if we aren't replacing any block.
            if (blockIdInPosition != 0) {
                return null
            }
            val facing = when (javaEntity.getByte("Facing").toInt()) {
                0, 1 -> return null // Facing down and up is unsupported by Bedrock Edition
                2 -> 3
                3 -> 2
                4 -> 5
                5 -> 4
                else -> 3
            }
            chunkSection.blocks[offset] = (199 and 0xFF).toByte()
            val halfOffset = offset / 2
            val isSecond = offset % 2 == 1
            val stored = if (!isSecond) {
                facing to (chunkSection.blockData[halfOffset].toInt() and 0x0F)
            } else {
                ((chunkSection.blockData[halfOffset].toInt() and 0xF0) shr 4) to facing
            }
            val first = stored.first and 0x0F
            val second = (stored.second and 0x0F) shl 4
            val merged = first or second
            chunkSection.blockData[halfOffset] = (merged and 0xFF).toByte()
            val tileEntity = NbtCompound(
                "id" to NbtString("ItemFrame"),
                "x" to NbtInt(tileX),
                "y" to NbtInt(tileY),
                "z" to NbtInt(tileZ),
                "isMoveable" to NbtByte(false)
            )
            tileEntity.copyFrom(javaEntity, "ItemDropChance")
            tileEntity.copyFrom(javaEntity, "ItemRotation")
            javaEntity.getNullableCompound("Item")?.toNukkitItem()?.let {
                tileEntity["Item"] = it
            }
            nukkitTileEntities[BlockPos(tileX, tileY, tileZ)] = tileEntity
            null
        }
        else -> null
    }
}

internal fun NbtCompound.entityToId(): Int? {
    val javaId = getNullableString("id")?.toLowerCase()?.removePrefix("minecraft:") ?: return null
    return java2bedrockEntities[javaId].takeUnless { it == 0 }
}
