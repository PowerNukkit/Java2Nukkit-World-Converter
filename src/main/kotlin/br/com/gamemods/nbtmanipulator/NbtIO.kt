@file:Suppress("unused")

package br.com.gamemods.nbtmanipulator

import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.reflect.KClass

object NbtIO {
    fun writeNbtFile(outputStream: OutputStream, file: NbtFile, compressed: Boolean = true) {
        val tag = file.tag
        val typeId = tag.typeId
        val serializer = serializers[typeId]
        val output = if (compressed) GZIPOutputStream(outputStream) else outputStream
        val dataOut = DataOutputStream(output)

        dataOut.writeByte(typeId)
        dataOut.writeUTF(file.name)

        serializer.writeTag(dataOut, tag)
        dataOut.flush()
        if (output is GZIPOutputStream) {
            output.finish()
        }
    }

    fun writeNbtFile(file: File, tag: NbtFile, compressed: Boolean = true) {
        file.outputStream().buffered().use { writeNbtFile(it, tag, compressed); it.flush() }
    }

    fun readNbtFile(inputStream: InputStream, compressed: Boolean = true): NbtFile {
        val input = if (compressed) GZIPInputStream(inputStream) else inputStream
        val dataIn = DataInputStream(input)

        val typeId = dataIn.read()
        val serializer = serializers[typeId]

        val name = dataIn.readUTF()

        return NbtFile(name, serializer.readTag(dataIn))
    }

    fun readNbtFile(file: File, compressed: Boolean = true): NbtFile {
        return file.inputStream().buffered().use { readNbtFile(it, compressed) }
    }
}


private val serializers = listOf(
    NbtEndSerial,
    NbtByteSerial,
    NbtShortSerial,
    NbtIntSerial,
    NbtLongSerial,
    NbtFloatSerial,
    NbtDoubleSerial,
    NbtByteArraySerial,
    NbtStringSerial,
    NbtListSerial,
    NbtCompoundSerial,
    NbtIntArraySerial,
    NbtLongArraySerial
)

private val NbtTag.typeId
    get() = serializers.indexOfFirst { it.kClass == this::class }

private sealed class NbtSerial<T: NbtTag> (val kClass: KClass<T>){
    abstract fun readTag(input: DataInputStream): T
    abstract fun writeTag(output: DataOutputStream, tag: T)

    @Suppress("UNCHECKED_CAST")
    @JvmName("writeRawTag")
    fun writeTag(output: DataOutputStream, tag: NbtTag) {
        writeTag(output, tag as T)
    }
}

private object NbtEndSerial: NbtSerial<NbtEnd>(NbtEnd::class) {
    override fun readTag(input: DataInputStream): NbtEnd {
        return NbtEnd
    }

    override fun writeTag(output: DataOutputStream, tag: NbtEnd) {
    }
}

private object NbtByteSerial: NbtSerial<NbtByte>(NbtByte::class) {
    override fun readTag(input: DataInputStream): NbtByte {
        return NbtByte(input.readByte())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtByte) {
        output.writeByte(tag.value.toInt())
    }
}

private object NbtShortSerial: NbtSerial<NbtShort>(NbtShort::class) {
    override fun readTag(input: DataInputStream): NbtShort {
        return NbtShort(input.readShort())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtShort) {
        output.writeShort(tag.value.toInt())
    }
}

private object NbtIntSerial: NbtSerial<NbtInt>(NbtInt::class) {
    override fun readTag(input: DataInputStream): NbtInt {
        return NbtInt(input.readInt())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtInt) {
        output.writeInt(tag.value)
    }
}

private object NbtLongSerial: NbtSerial<NbtLong>(NbtLong::class) {
    override fun readTag(input: DataInputStream): NbtLong {
        return NbtLong(input.readLong())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtLong) {
        output.writeLong(tag.value)
    }
}

private object NbtFloatSerial: NbtSerial<NbtFloat>(NbtFloat::class) {
    override fun readTag(input: DataInputStream): NbtFloat {
        return NbtFloat(input.readFloat())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtFloat) {
        output.writeFloat(tag.value)
    }
}

private object NbtDoubleSerial: NbtSerial<NbtDouble>(NbtDouble::class) {
    override fun readTag(input: DataInputStream): NbtDouble {
        return NbtDouble(input.readDouble())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtDouble) {
        output.writeDouble(tag.value)
    }
}

private object NbtByteArraySerial: NbtSerial<NbtByteArray>(NbtByteArray::class) {
    override fun readTag(input: DataInputStream): NbtByteArray {
        val size = input.readInt()
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return NbtByteArray(bytes)
    }

    override fun writeTag(output: DataOutputStream, tag: NbtByteArray) {
        output.writeInt(tag.value.size)
        output.write(tag.value)
    }
}

private object NbtStringSerial: NbtSerial<NbtString>(NbtString::class) {
    override fun readTag(input: DataInputStream): NbtString {
        return NbtString(input.readUTF())
    }

    override fun writeTag(output: DataOutputStream, tag: NbtString) {
        output.writeUTF(tag.value)
    }
}

private object NbtListSerial: NbtSerial<NbtList<*>>(NbtList::class) {
    override fun readTag(input: DataInputStream): NbtList<*> {
        val type = input.read()
        val size = input.readInt()
        if (type == 0 && size > 0) {
            error("Missing type on NbtList")
        }
        val serializer = serializers[type]
        val list = mutableListOf<NbtTag>()
        for (i in 1..size) {
            list += serializer.readTag(input)
        }
        return NbtList(list)
    }

    override fun writeTag(output: DataOutputStream, tag: NbtList<*>) {
        val sample = tag.value.firstOrNull() ?: NbtEnd
        val typeId = sample.typeId
        val serializer = serializers[typeId]

        if (typeId == 0 && tag.value.size > 0) {
            error("NbtList cannot have NbtEnd")
        }

        output.writeByte(typeId)
        output.writeInt(tag.value.size)
        tag.value.forEach {
            serializer.writeTag(output, it)
        }
    }
}

private object NbtCompoundSerial: NbtSerial<NbtCompound>(NbtCompound::class) {
    override fun readTag(input: DataInputStream): NbtCompound {
        val map = mutableMapOf<String, NbtTag>()
        while (true) {
            val typeId = input.read()
            if (typeId == 0) {
                break
            }

            val name = input.readUTF()
            val serializer = serializers[typeId]
            val childTag = serializer.readTag(input)
            map[name] = childTag
        }
        return NbtCompound(map)
    }

    override fun writeTag(output: DataOutputStream, tag: NbtCompound) {
        check(tag.value.values.none { it == NbtEnd }) {
            "NbtCompound cannot have an NbtEnd"
        }

        tag.value.forEach { (name, childTag) ->
            val typeId = childTag.typeId
            val serializer = serializers[typeId]
            output.writeByte(typeId)
            output.writeUTF(name)
            serializer.writeTag(output, childTag)
        }

        output.writeByte(0)
    }
}

private object NbtIntArraySerial: NbtSerial<NbtIntArray>(NbtIntArray::class) {
    override fun readTag(input: DataInputStream): NbtIntArray {
        val size = input.readInt()
        val array = IntArray(size)
        for (i in 0 until size) {
            array[i] = input.readInt()
        }
        return NbtIntArray(array)
    }

    override fun writeTag(output: DataOutputStream, tag: NbtIntArray) {
        output.writeInt(tag.value.size)
        tag.value.forEach {
            output.writeInt(it)
        }
    }
}

private object NbtLongArraySerial: NbtSerial<NbtLongArray>(NbtLongArray::class) {
    override fun readTag(input: DataInputStream): NbtLongArray {
        val size = input.readInt()
        val array = LongArray(size)
        for (i in 0 until size) {
            array[i] = input.readLong()
        }
        return NbtLongArray(array)
    }

    override fun writeTag(output: DataOutputStream, tag: NbtLongArray) {
        output.writeInt(tag.value.size)
        tag.value.forEach {
            output.writeLong(it)
        }
    }
}