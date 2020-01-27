package br.com.gamemods.j2nwc.internal

import br.com.gamemods.j2nwc.WorldConverter
import java.util.*

internal object IdComparator: Comparator<Map.Entry<String, String>> {
    override fun compare(entry1: Map.Entry<String, String>, entry2: Map.Entry<String, String>): Int {
        val (blockId1, blockData1) = entry1.value.split(',', limit = 2).map { it.toInt() }
        val (blockId2, blockData2) = entry2.value.split(',', limit = 2).map { it.toInt() }
        return blockId1.compareTo(blockId2).takeIf { it != 0 }
            ?: blockData1.compareTo(blockData2).takeIf { it != 0 }
            ?: entry1.key.compareTo(entry2.key)
    }
}

internal object TypeIdComparator: Comparator<Map.Entry<String, String>> {
    override fun compare(entry1: Map.Entry<String, String>, entry2: Map.Entry<String, String>): Int {
        val (type1, blockId1, blockData1) = entry1.value.split(',', limit = 3)
        val (type2, blockId2, blockData2) = entry2.value.split(',', limit = 3)
        return type1.compareTo(type2).takeIf { it != 0 }
            ?: blockId1.toInt().compareTo(blockId2.toInt()).takeIf { it != 0 }
            ?: (blockData1.takeIf { it != "~" }?.toInt() ?: -1)
                .compareTo(blockData2.takeIf { it != "~" }?.toInt() ?: -1).takeIf { it != 0 }
            ?: entry1.key.compareTo(entry2.key)
    }
}

internal fun checkIds(targetType: WorldConverter.TargetType) {
    val validBlockPattern = Regex("""^\d+,\d+$""")
    java2bedrockStates.values.find { !validBlockPattern.matches(it) }?.let {
        error("Found an invalid mapping at block-states.properties: $it")
    }


    val validKeyPattern = Regex("""^(B,\d+,\d+)|(I,\d+,(\d+|~))$""")
    java2bedrockItems.values.find { !validKeyPattern.matches(it) }?.let {
        error("Found an invalid mapping at items.properties: $it")
    }

    val validItemValuePattern = Regex("""^\d+,(\d+|~)$""")
    val bedrock2target = targetType.bedrock2target
    bedrock2target.forEach { k, v ->
        val key = k.toString()
        if (!validKeyPattern.matches(key)) {
            error("Found an invalid key at bedrock-2-nukkit.properties: $key")
        }

        if (key[0] == 'B') {
            if (!validBlockPattern.matches(v.toString())) {
                error("Found an invalid value at bedrock-2-nukkit.properties: Key:$key Value:$v")
            }
        } else {
            if (!validItemValuePattern.matches(v.toString())) {
                error("Found an invalid value at bedrock-2-nukkit.properties: Key:$key Value:$v")
            }
        }
    }

    val blockNames = targetType.blockNames
    val validDamage = 0..targetType.maxDataValue
    java2bedrockStates.asSequence().sortedWith(IdComparator).forEach { (state, stateMapping) ->
        val (mappedBlockId, mappedBlockData) = stateMapping.split(',', limit = 2).map { it.toInt() }
        val (nukkitBlockId, nukkitBlockData) =
            (bedrock2target["B,$mappedBlockId,$mappedBlockData"]?.toString() ?: stateMapping)
                .split(',', limit = 2).map { it.toInt() }

        if (nukkitBlockId !in blockNames) {
            error("The block $nukkitBlockId,$nukkitBlockData is unsupported by the target type $targetType!\nState: $state")
        }

        if (nukkitBlockData !in validDamage) {
            error("The block $nukkitBlockId,$nukkitBlockData has data out of range 0..${targetType.maxDataValue} " +
                    "and is unsupported by the target type $targetType!\nState: $state")
        }
    }

    val itemNames = targetType.itemNames
    val suprpessWarningItemIds = targetType.suprpessWarningItemIds
    java2bedrockItems.asSequence().sortedWith(TypeIdComparator).forEach { (item, stateMapping) ->
        val (type, mappedItemId, mappedItemData) = stateMapping.split(',', limit = 3)
        val (nukkitItemId, nukkitItemData) =
            (bedrock2target["$type,$mappedItemId,$mappedItemData"]?.toString()
                ?: bedrock2target["$type,$mappedItemId,~"]?.toString()
                ?: "$mappedItemId,$mappedItemData")
                .split(',', limit = 2).mapIndexed { i, str->
                    when {
                        i == 0 -> str
                        str == "~" -> mappedItemData
                        else -> str
                    }
                }

        if (type == "I") {

            val itemId = nukkitItemId.toInt()
            if (itemId !in suprpessWarningItemIds && itemId !in itemNames) {
                println("The item $type,$nukkitItemId,$nukkitItemData is unsupported by the target type $targetType!\nItem: $item")
            }

        } else {
            if (nukkitItemId.toInt() !in blockNames) {
                println("The item-block $type,$nukkitItemId,$nukkitItemData is unsupported the target type $targetType!\nItem: $item")
            }
        }
    }
}
