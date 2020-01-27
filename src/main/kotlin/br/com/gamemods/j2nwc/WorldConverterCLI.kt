package br.com.gamemods.j2nwc

import br.com.gamemods.j2nwc.internal.JavaChunk
import br.com.gamemods.j2nwc.internal.checkIds
import kotlinx.cli.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * A JAR entry point to the Command Line Interface functionality of Java to Nukkit World Converter.
 */
object WorldConverterCLI {

    /**
     * Executes the tool as CLI.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val jarFileName = JavaChunk::class.java.protectionDomain.codeSource.location.toURI().path.substringAfterLast('/')

        val cli = CommandLineInterface("java -jar $jarFileName")
        val from by cli.positionalArgument("from-dir", "The world Java Edition world directory")
        val to by cli.positionalArgument("to-dir", "The location where the Nukkit world will be created")
        val regionsArg by cli.flagValueArgument(
            "-r",
            "regions",
            "A list of region positions that will be converted. Example: -r 0,0;-1,0;-1,-1"
        )
        val keepCustomHeads by cli.flagArgument(
            "--keep-custom-heads",
            "Makes the tool convert player heads with custom skins as regular player heads. The default behaviour is to remove them."
        )
        val target by cli.flagValueArgument(
            "--target",
            "nukkit-implementation",
            "The target output format. Valid values are nukkit (for Nukkit v1.X), nukkit2 (for Nukkit v2.X), " +
                    "powernukkit (for PowerNukkit v1.X) and powernukkit2 (for PowerNukkit v2.X)",
            "nukkit",
            { it.trim().toLowerCase() }
        )

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

        val targetType = when (target) {
            "nukkit" -> WorldConverter.TargetType.NUKKIT
            "powernukkit" -> WorldConverter.TargetType.POWER_NUKKIT
            "nukkit2", "powernukkit2" -> {
                System.err.println("Direct world conversion to LevelDB isn't supported yet, sorry. " +
                        "Try ${target.substring(0, target.length - 1)}, the server should update to the 2.0 automatically.")
                exitProcess(7)
            }
            else -> {
                System.err.println("Unknown target type $target, valid targets are nukkit, nukkit2, powernukkit, powernukkit2")
                cli.printHelp()
                exitProcess(8)
            }
        }

        checkIds(targetType)

        val regionLimit = if (regionsArg == null) {
            emptyList()
        } else {
            val regions = regionsArg!!
            if (!regions.matches(Regex("""^(-?\d+,-?\d+)(;-?\d+,-?\d+)*$"""))) {
                System.err.println("The regions parameter must follow this syntax:\n-r 0,0;-1,0;-1,-1")
                cli.printHelp()
                exitProcess(6)
            } else {
                regions.split(';').asSequence()
                    .map { it.split(',') }
                    .map { RegionPosition(it[0].toInt(), it[1].toInt()) }
                    .toList()
            }
        }

        val fromPath = Paths.get(from!!)
        if (!Files.isDirectory(fromPath)) {
            System.err.println("$from is not a folder!")
            cli.printHelp()
            exitProcess(4)
        }

        val toPath = Paths.get(to!!)
        try {
            WorldConverter(fromPath.toFile(), toPath.toFile()).apply {
                regions = regionLimit.toMutableSet()
                skipSkinHeads = !keepCustomHeads
                this.targetType = targetType
                convert()
            }
            println("The world has been converted successfully")
        } catch (e: Exception) {
            System.err.println("An error has occurred while converting the world!")
            e.printStackTrace(System.err)
            println("The world conversion has failed")
            exitProcess(5)
        }
    }
}
