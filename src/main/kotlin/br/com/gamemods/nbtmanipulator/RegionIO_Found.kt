package br.com.gamemods.nbtmanipulator

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

object RegionIO_Found {
    private const val SECTOR_BYTES = 4096

    fun writeFile(f: File, region: Region) {
        val chunkLocations = IntArray(region.size)
    }

    fun readFile(f: File): Region {
        RandomAccessFile(f, "r").use { raf ->
            // If there isn't a header and at least one chunk sector, the region is empty
            if (raf.length() < SECTOR_BYTES * 3) {
                return Region(RegionPos(0, 0))
            }
            // Each chunk can use 1 or more sectors, and the first two sectors
            // are the header, so this is the maximum number of chunks
            val maxChunks = raf.length().toInt() / SECTOR_BYTES - 2
            val chunkLocation = IntArray(maxChunks)
            var entries = 0
            for (i in 0 until SECTOR_BYTES / 4) {
                val offset = raf.readInt()
                if (offset != 0) {
                    // The rest of the offset is the number of sectors that the chunk
                    // occupies.  We don't care about that as each chunk stores its length
                    chunkLocation[entries++] = (offset shr 8) * SECTOR_BYTES
                }
            }
            val region = Region(RegionPos(0, 0))

            for (i in 0 until entries) {
                region.add(readChunk(raf, chunkLocation[i]))
            }

            return region
        }
    }

    private fun readChunk(raf: RandomAccessFile, location: Int): Chunk {
        raf.seek(location.toLong())
        val length = raf.readInt()
        val compressionType = raf.readByte()
        val data = ByteArray(length - 1)
        raf.readFully(data)
        return Chunk(Date(), readTag(decompress(compressionType.toInt(), data)))
    }

    private fun decompress(type: Int, data: ByteArray): InputStream {
        return when (type) {
            1 -> GZIPInputStream(ByteArrayInputStream(data))
            2 -> InflaterInputStream(ByteArrayInputStream(data))
            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    private fun readTag(`in`: InputStream): NbtFile {
        return NbtIO.readNbtFile(`in`, false)
    }
}