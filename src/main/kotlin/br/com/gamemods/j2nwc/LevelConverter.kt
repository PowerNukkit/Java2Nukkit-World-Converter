@file:JvmName("LevelConverter")
package br.com.gamemods.j2nwc

import kotlinx.cli.CommandLineInterface
import kotlinx.cli.parse
import kotlinx.cli.positionalArgument
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    checkIds()

    val jarFileName = JavaChunk::class.java.protectionDomain.codeSource.location.toURI().path

    val cli = CommandLineInterface("java -jar $jarFileName")
    val from by cli.positionalArgument("from-dir", "The world Java Edition world directory")
    val to by cli.positionalArgument("to-dir", "The location where the Nukkit world will be created")

    try {
        cli.parse(args)
    } catch (e: Exception) {
        exitProcess(1)
    }

    if (from == null) {
        System.err.println("The from-dir argument was not specified")
        cli.printHelp()
        exitProcess(2)
    }

    if (to == null) {
        System.err.println("The to-dir argument was not specified")
        cli.printHelp()
        exitProcess(3)
    }

    val fromPath = Paths.get(from)
    if (!Files.isDirectory(fromPath)) {
        System.err.println("$from is not a folder!")
        cli.printHelp()
        exitProcess(4)
    }

    val toPath = Paths.get(to)
    try {
        WorldConverter(fromPath.toFile(), toPath.toFile()).convert()
        println("The world has been converted successfully")
    } catch (e: Exception) {
        System.err.println("An error has occurred while converting the world!")
        e.printStackTrace(System.err)
        println("The world conversion has failed")
        exitProcess(5)
    }
}
