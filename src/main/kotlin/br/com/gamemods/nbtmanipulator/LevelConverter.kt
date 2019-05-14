@file:JvmName("LevelConverter")
package br.com.gamemods.nbtmanipulator

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size != 2) {
        error("Especificar apenas 2 argumentos de-para")
    }
    val from = args[0]
    val to = args[1]
    val fromPath = Paths.get(from)
    if (!Files.isDirectory(fromPath)) {
        error("$from não é uma pasta!")
    }

    val toPath = Paths.get(to)
    if (Files.exists(toPath)) {
        //error("$to já existe!")
    }

    WorldConverter(fromPath.toFile(), toPath.toFile()).convert()
}
