package br.com.gamemods.nbtmanipulator

class PackedIntegerArray {
    private val storage: LongArray
    private val elementBits: Int
    private val maxValue: Long
    private val size: Int

    constructor(int_1: Int, int_2: Int): this(int_1, int_2, LongArray(roundUp(int_2 * int_1, 64) / 64))

    constructor(int_1: Int, int_2: Int, longs_1: LongArray) {
        this.size = int_2
        this.elementBits = int_1
        this.storage = longs_1
        this.maxValue = (1L shl int_1) - 1L
        val int_3 = roundUp(int_2 * int_1, 64) / 64
        if (longs_1.size != int_3) {
            throw RuntimeException("Invalid length given for storage, got: " + longs_1.size + " but expected: " + int_3)
        }
    }

    operator fun get(int_1: Int): Int {
        val int_2 = int_1 * this.elementBits
        val int_3 = int_2 shr 6
        val int_4 = (int_1 + 1) * this.elementBits - 1 shr 6
        val int_5 = int_2 xor (int_3 shl 6)
        if (int_3 == int_4) {
            return (this.storage[int_3].ushr(int_5) and this.maxValue).toInt()
        } else {
            val int_6 = 64 - int_5
            return (this.storage[int_3].ushr(int_5) or (this.storage[int_4] shl int_6) and this.maxValue).toInt()
        }
    }
    companion object {
        private fun roundUp(int_1: Int, int_2: Int): Int {
            var int_2 = int_2
            if (int_2 == 0) {
                return 0
            } else if (int_1 == 0) {
                return int_2
            } else {
                if (int_1 < 0) {
                    int_2 *= -1
                }

                val int_3 = int_1 % int_2
                return if (int_3 == 0) int_1 else int_1 + int_2 - int_3
            }
        }
    }
}