package br.com.gamemods.nbtmanipulator

import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.*
import kotlin.math.ceil
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater



object RegionIO {
    fun readRegion(file: File): Region {
        val nameParts = file.name.split('.', limit = 4)
        val xPos = nameParts[1].toInt()
        val zPos = nameParts[2].toInt()
        val regionPos = RegionPos(xPos, zPos)
        return readRegion(file, regionPos)
    }

    data class ChunkInfo(val location: Int, val size: Int, var lastModified: Date = Date(0))

    fun readRegion(file: File, pos: RegionPos): Region {

        RandomAccessFile(file, "r").use { input ->
            val chunkInfos = Array(1024) {
                val loc = (input.read() shl 16) + (input.read() shl 8) + input.read()
                ChunkInfo(loc * 4096, input.read() * 4096).takeUnless { it.size == 0 }
            }

            for (i in 0 until 1024) {
                input.readInt().takeUnless { it == 0 }?.let {
                    chunkInfos[i]!!.lastModified = Date( it * 1000L)
                }
            }

            val chunks = chunkInfos.mapIndexed { i, ci ->
                val info = ci ?: return@mapIndexed null
                input.seek(info.location.toLong())
                val length = input.readInt()
                val compression = input.read()
                check(compression == 1 || compression == 2) {
                    "Bad compression $compression . Chunk index: $i"
                }

                val data = ByteArray(length)
                input.readFully(data)

                val inputStream = when (compression) {
                    1 -> GZIPInputStream(ByteArrayInputStream(data))
                    2 -> InflaterInputStream(ByteArrayInputStream(data))
                    else -> error("Unexpected compression type $compression")
                }

                val nbt = NbtIO.readNbtFile(inputStream, false)
                Chunk(info.lastModified, nbt)
            }


            val region = Region(pos, chunks)
            return region
        }
    }

    fun deflate(data: ByteArray, level: Int): ByteArray {
        val deflater = Deflater(level)
        deflater.reset()
        deflater.setInput(data)
        deflater.finish()
        val bos = ByteArrayOutputStream(data.size)
        val buf = ByteArray(1024)
        try {
            while (!deflater.finished()) {
                val i = deflater.deflate(buf)
                bos.write(buf, 0, i)
            }
        } finally {
            deflater.end()
        }
        return bos.toByteArray()
    }

    fun writeRegion(file: File, region: Region) {
        val chunkInfoHeader = mutableListOf<ChunkInfo>()

        val heapData = ByteArrayOutputStream()
        val heap = DataOutputStream(heapData)
        var heapPos = 0
        var index = -1
        for (z in 0 until 32) {
            for (x in 0 until 32) {
                index++
                val pos = ChunkPos(region.position.xPos * 32 + x, region.position.zPos * 32 + z)
                val chunk = region[pos]

                if (chunk == null) {
                    chunkInfoHeader += ChunkInfo(0, 0)
                } else {
                    val chunkData = ByteArrayOutputStream()
                    //val chunkOut = DeflaterOutputStream(chunkData)
                    val chunkOut = chunkData
                    NbtIO.writeNbtFile(chunkOut, chunk.nbtFile, false)
                    //chunkOut.finish()
                    chunkOut.flush()
                    chunkOut.close()
                    val uncompressedChunkBytes = chunkData.toByteArray()
                    val chunkBytes = deflate(uncompressedChunkBytes, 7)
                    val sectionBytes = ByteArray((ceil((chunkBytes.size + 5) / 4096.0).toInt() * 4096) - 5) {
                        if (it >= chunkBytes.size) {
                            0
                        } else {
                            chunkBytes[it]
                        }
                    }

                    heap.writeInt(chunkBytes.size + 1)
                    heap.writeByte(2)
                    heap.write(sectionBytes)
                    chunkInfoHeader += ChunkInfo(8192 + heapPos, sectionBytes.size + 5, chunk.lastModified)
                    heapPos += 5 + sectionBytes.size
                }
            }
        }
        heap.flush()
        heap.close()
        val heapBytes = heapData.toByteArray()

        val headerData = ByteArrayOutputStream()
        val header = DataOutputStream(headerData)

        chunkInfoHeader.forEach {
            assert(ByteBuffer.wrap(heapBytes, it.location - 8192, 4).int > 0) {
                "Header location is pointing to an incorrect heap location"
            }
            val sec = it.location / 4096
            header.writeByte((sec shr 16) and 0xFF)
            header.writeByte((sec shr 8) and 0xFF)
            header.writeByte(sec and 0xFF)

            val size = it.size / 4096
            header.writeByte(size)
        }

        chunkInfoHeader.forEach {
            header.writeInt((it.lastModified.time / 1000L).toInt())
        }
        header.close()
        val headerBytes = headerData.toByteArray()
        check(headerBytes.size == 8192) {
            "Failed to write the mca header. Size ${header.size()} != 4096"
        }

        file.outputStream().buffered().use {
            it.write(headerBytes)
            it.write(heapBytes)
            it.flush()
        }
    }
}