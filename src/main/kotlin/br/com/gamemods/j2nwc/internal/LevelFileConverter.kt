package br.com.gamemods.j2nwc.internal

import br.com.gamemods.j2nwc.WorldConverter
import br.com.gamemods.nbtmanipulator.*
import java.io.File

internal fun convertLevelFile(from: File, to: File, worldConverter: WorldConverter) {
    val input = NbtIO.readNbtFile(from)
    val inputData = input.compound.getCompound("Data")

    val outputData = NbtCompound()
    outputData.copyFrom(inputData, "GameRules")
    outputData.copyFrom(inputData, "DayTime")
    outputData.copyFrom(inputData, "GameType")
    val generatorName = inputData.getNullableString("generatorName") ?: "normal"
    outputData["generatorName"] = generatorName
    outputData["generatorVersion"] = 1
    inputData.getNullableCompound("generatorOptions")?.convertGeneratorOptions(generatorName, worldConverter)?.also {
        outputData["generatorOptions"] = it
    }
    outputData["hardcore"] = false
    outputData["initialized"] = false
    outputData.copyFrom(inputData, "LastPlayed")
    outputData.copyFrom(inputData, "LevelName")
    outputData.copyFrom(inputData, "raining")
    outputData.copyFrom(inputData, "rainTime")
    outputData.copyFrom(inputData, "RandomSeed")
    outputData.copyFrom(inputData, "SizeOnDisk")
    outputData.copyFrom(inputData, "SpawnX")
    outputData.copyFrom(inputData, "SpawnY")
    outputData.copyFrom(inputData, "SpawnZ")
    outputData.copyFrom(inputData, "thundering")
    outputData.copyFrom(inputData, "thunderTime")
    outputData.copyFrom(inputData, "Time")
    outputData.copyFrom(inputData, "version")

    val output = NbtCompound("Data" to outputData)
    val file = NbtFile("", output)
    NbtIO.writeNbtFile(to, file)
}

private fun NbtCompound.convertGeneratorOptions(
    generatorName: String,
    worldConverter: WorldConverter
): String? {
    // Nukkit only supports presets (aka generatorOptions) in flat worlds. It also have a limited support to generators.
    if (generatorName != "flat") {
        return null
    }

    val preset = StringBuilder("2;")
    val layers = getNullableCompoundList("layers") ?: return null
    layers.forEach { layer ->
        val blockName = layer.getNullableString("block") ?: return@forEach
        val heightNbt = layer["height"]
        val height = when (heightNbt) {
            is NbtInt -> heightNbt.value
            is NbtByte -> heightNbt.value.toInt()
            else -> 1
        }
        val javaBlock = JavaBlock(BlockPos(0, height, 0), JavaPalette(blockName, null), null)
        val nukkitBlock = javaBlock.toNukkit(mutableListOf(), mutableListOf(), worldConverter)
        for (i in 1..height) {
            preset.append(nukkitBlock.blockData.blockId)
            if (nukkitBlock.blockData.data != 0) {
                preset.append(':').append(nukkitBlock.blockData.data)
            }
            preset.append(',')
        }
    }
    if (preset.endsWith(',')) {
        preset.setLength(preset.length - 1)
    }
    preset.append(';')
    val biomeName = getNullableString("biome")?.removePrefix("minecraft:")
    val nukkitBiome = if (biomeName != null) {
        val remap = javaBiomesString2Bedrock[biomeName]
        if (remap == null) {
            System.err.println("Unmapped biome with name $biomeName")
        }
        remap ?: 1
    } else {
        1
    }
    preset.append(nukkitBiome)

    //structures are not supported by Nukkit's flat worlds
    return preset.toString()
}
