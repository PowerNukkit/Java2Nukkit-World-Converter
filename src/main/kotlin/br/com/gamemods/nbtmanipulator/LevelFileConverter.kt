package br.com.gamemods.nbtmanipulator

import java.io.File

fun NbtCompound.copy(other: NbtCompound, tagName: String, default: NbtTag? = null) {
    val tag = other[tagName] ?: default
    if (tag != null) {
        set(tagName, tag)
    }
}

internal fun convertLevelFile(from: File, to: File) {
    val input = NbtIO.readNbtFile(from)
    val inputData = input.compound.getCompound("Data")

    val outputData = NbtCompound()
    outputData.copy(inputData, "GameRules")
    outputData.copy(inputData, "DayTime")
    outputData.copy(inputData, "GameType")
    outputData.copy(inputData, "generatorName")
    outputData.copy(inputData, "generatorVersion")
    outputData.copy(inputData, "generatorVersion")
    outputData.copy(inputData, "generatorOptions", NbtString(""))
    outputData["hardcore"] = NbtByte(0)
    outputData["initialized"] = NbtByte(0)
    outputData.copy(inputData, "LastPlayed")
    outputData.copy(inputData, "LevelName")
    outputData.copy(inputData, "raining")
    outputData.copy(inputData, "rainTime")
    outputData.copy(inputData, "RandomSeed")
    outputData.copy(inputData, "SizeOnDisk")
    outputData.copy(inputData, "SpawnX")
    outputData.copy(inputData, "SpawnY")
    outputData.copy(inputData, "SpawnZ")
    outputData.copy(inputData, "thundering")
    outputData.copy(inputData, "thunderTime")
    outputData.copy(inputData, "Time")
    outputData.copy(inputData, "version")

    val output = NbtCompound("Data" to outputData)
    val file = NbtFile("", output)
    NbtIO.writeNbtFile(to, file)
}