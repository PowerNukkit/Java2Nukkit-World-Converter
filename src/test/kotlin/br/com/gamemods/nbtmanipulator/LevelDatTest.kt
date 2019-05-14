package br.com.gamemods.nbtmanipulator

import org.junit.Test

class LevelDatTest {
    @Test
    fun testReadLevelDat() {
        val compound = LevelDatTest::class.java.getResourceAsStream("/level.dat").use { inputStream ->
            NbtIO.readNbtFile(inputStream)
        }

        println(compound)
    }
}