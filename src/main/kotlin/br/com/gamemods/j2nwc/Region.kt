package br.com.gamemods.j2nwc

import kotlin.math.floor

class Region(val position: RegionPos): AbstractMutableMap<ChunkPos, Chunk>() {
    private val chunks = Array<Chunk?>(1024) { null }

    constructor(position: RegionPos, chunks: List<Chunk?>): this(position) {
        addAll(chunks)
    }

    override fun put(key: ChunkPos, value: Chunk): Chunk? {
        val regX = floor(key.xPos / 32.0).toInt()
        val regZ = floor(key.zPos / 32.0).toInt()
        check(regX == position.xPos && regZ == position.zPos) {
            "The chunk $key is not part of the region $position. It's part of r.$regX.$regZ.mca"
        }
        val offset = offset(key)
        val before = chunks[offset]
        chunks[offset] = value
        return before
    }

    override fun get(key: ChunkPos): Chunk? {
        val regX = floor(key.xPos / 32.0).toInt()
        val regZ = floor(key.zPos / 32.0).toInt()
        if (regX != position.xPos || regZ != position.zPos) {
            return null
        }

        return chunks[offset(key)]
    }

    override fun remove(key: ChunkPos): Chunk? {
        val offset = offset(key)
        val before = chunks[offset]
        chunks[offset] = null
        return before
    }

    override fun remove(key: ChunkPos, value: Chunk): Boolean {
        val offset = offset(key)
        val before = chunks[offset]
        return if (value == before) {
            chunks[offset] = null
            true
        } else {
            false
        }
    }

    private fun offset(chunkPos: ChunkPos) = internalOffset(chunkPos.xPos - position.xPos * 32, chunkPos.zPos - position.zPos * 32)
    private fun internalOffset(x: Int, z: Int) = ((x % 32) + (z % 32) * 32)

    fun add(chunk: Chunk) {
        put(chunk.position, chunk)
    }

    @JvmName("addAllNotNull")
    fun addAll(chunks: List<Chunk>) {
        chunks.forEach(this::add)
    }

    @JvmName("addAllNullable")
    fun addAll(chunks: List<Chunk?>) {
        chunks.asSequence().filterNotNull().forEach(this::add)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<ChunkPos, Chunk>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<ChunkPos, Chunk>>() {
            override val size: Int
                get() = chunks.count { it != null }

            override fun add(element: MutableMap.MutableEntry<ChunkPos, Chunk>): Boolean {
                val removed = put(element.key, element.value)
                return removed != element.value
            }

            override fun iterator(): MutableIterator<MutableMap.MutableEntry<ChunkPos, Chunk>> {
                return object : MutableIterator<MutableMap.MutableEntry<ChunkPos, Chunk>> {
                    lateinit var current: MutableMap.MutableEntry<ChunkPos, Chunk>
                    val iter = chunks.asSequence().filterNotNull().map {  chunk ->
                        object : MutableMap.MutableEntry<ChunkPos, Chunk> {
                            override val key: ChunkPos = chunk.position
                            override val value: Chunk
                                get() = get(key) ?: chunk

                            override fun setValue(newValue: Chunk): Chunk {
                                return put(key, newValue) ?: chunk
                            }
                        }
                    }.iterator()

                    override fun hasNext(): Boolean {
                        return iter.hasNext()
                    }

                    override fun next(): MutableMap.MutableEntry<ChunkPos, Chunk> {
                        current = iter.next()
                        return current
                    }

                    override fun remove() {
                        this@Region.remove(current.key)
                    }
                }
            }
        }
}