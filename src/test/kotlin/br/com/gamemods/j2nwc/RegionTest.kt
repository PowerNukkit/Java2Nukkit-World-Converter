package br.com.gamemods.j2nwc

import br.com.gamemods.j2nwc.internal.toNukkit
import br.com.gamemods.regionmanipulator.RegionIO
import br.com.gamemods.regionmanipulator.RegionPos
import org.junit.Test
import java.io.File

class RegionTest {
    @Test
    fun testReadMCA() {
        val tempFile = File.createTempFile("r.1,-1,", ".mca")
        tempFile.deleteOnExit()
        RegionTest::class.java.getResourceAsStream("/r.1.-1.mca").use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val mca = RegionIO.readRegion(tempFile, RegionPos(1, -1))
        mca.toNukkit(mutableListOf())
    }

}
