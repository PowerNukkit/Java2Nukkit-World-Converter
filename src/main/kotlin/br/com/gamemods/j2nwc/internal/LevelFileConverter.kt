package br.com.gamemods.j2nwc.internal

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import java.io.File

internal fun convertLevelFile(from: File, to: File) {
    val input = NbtIO.readNbtFile(from)
    val inputData = input.compound.getCompound("Data")

    val outputData = NbtCompound()
    outputData.copyFrom(inputData, "GameRules")
    outputData.copyFrom(inputData, "DayTime")
    outputData.copyFrom(inputData, "GameType")
    outputData.copyFrom(inputData, "generatorName")
    outputData.copyFrom(inputData, "generatorVersion")
    outputData.copyFrom(inputData, "generatorVersion")
    //outputData["generatorOptions"] = ""
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
