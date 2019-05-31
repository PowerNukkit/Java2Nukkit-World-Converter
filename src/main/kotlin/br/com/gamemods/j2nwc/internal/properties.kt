package br.com.gamemods.j2nwc.internal

import java.util.*

private fun properties(name: String) = Properties().apply {
    JavaPalette::class.java.getResourceAsStream(name).bufferedReader().use {
        load(it)
    }
}

private fun propertiesStringString(name: String) = properties(name)
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString() }

private fun propertiesStringInt(name: String) = properties(name)
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString().toInt() }

internal val java2bedrockEntities = propertiesStringInt("/entity-ids.properties")

internal val java2bedrockStates = propertiesStringString("/block-states.properties").entries.asSequence().flatMap { e->
    if (";waterlogged-false" in e.key) {
        sequenceOf(e.key to e.value, e.key.replace(";waterlogged-false", "") to e.value)
    } else {
        sequenceOf(e.toPair())
    }
}.flatMap { e ->
    if (";powered-false" in e.first && e.first.matches(Regex("\\w+_trapdoor;"))) {
        sequenceOf(e, e.first.replace(";powered-false", "") to e.second)
    } else {
        sequenceOf(e)
    }
}.toMap()

internal val bedrock2nukkit = properties("/bedrock-2-nukkit.properties")

internal val java2bedrockItems = propertiesStringString("/items.properties")

internal val nukkitBlockIds = propertiesStringInt("/nukkit-block-ids.properties")

internal val nukkitItemIds = propertiesStringInt("/nukkit-item-ids.properties")

internal val nukkitBlockNames = nukkitBlockIds.entries.asSequence()
    .map { (k,v) -> k to v }.groupBy { (_,v) -> v }
    .mapValues { it.value.map { p-> p.first } }

internal val nukkitItemNames = nukkitItemIds.entries.asSequence()
    .map { (k,v) -> k to v }.groupBy { (_,v) -> v }
    .mapValues { it.value.map { p-> p.first } }
    .let {
        mapOf(0 to "air") + it
    }

internal val javaStatusEffectNames = properties("/status-effect-java-ids.properties")
    .mapKeys { it.key.toString().toInt() }.mapValues { it.value.toString().toLowerCase() }

internal val javaStatusEffectIds = javaStatusEffectNames.entries.associate { it.value to it.key }
internal val java2bedrockEffectIds = propertiesStringInt("/status-effect-ids.properties")

internal val javaTags = properties("/tags.properties")
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { entry ->
        entry.value.toString().split(',').map { it.trim() }
    }.let { tags2bedrock ->
        val mutable = tags2bedrock.toMutableMap()
        while (mutable.values.any { list-> list.any { it.startsWith("#") } }) {
            mutable.iterator().forEach { entry ->
                if (entry.value.any { it.startsWith('#') }) {
                    entry.setValue(entry.value.flatMap {
                        if (it.startsWith('#')) {
                            (mutable[it.substring(1)]?.asSequence() ?: sequenceOf()).asIterable()
                        } else {
                            sequenceOf(it).asIterable()
                        }
                    }.toSet().toList())
                }
            }
        }
        mutable
    }

internal val javaTags2Bedrock = javaTags.mapValues { entry ->
    entry.value.asSequence().flatMap { javaBlock ->
        val (bedrockId, bedrockData) = java2bedrockStates[javaBlock]?.split(',', limit = 2) ?: listOf("0","0").also {
            //println("The tag ${entry.key} points to a missing block $javaBlock")
        }
        val (nukkitId, _) = (
                bedrock2nukkit.getProperty("B,$bedrockId,$bedrockData") ?: "$bedrockId,$bedrockData"
                ).split(',', limit = 2)
        if (nukkitId != "0") {
            nukkitBlockNames[nukkitId.toInt()]?.asSequence() ?: sequenceOf(nukkitId, javaBlock)
        } else {
            sequenceOf(null)
        }
    }.filterNotNull().toList()
}

internal val javaBlockProps2Bedrock = javaTags2Bedrock + java2bedrockStates.asSequence().filter { ';' !in it.key }.flatMap { (javaBlock, mapping) ->
    val (bedrockId, bedrockData) = mapping.split(',', limit = 2)
    val (nukkitId, _) = (
            bedrock2nukkit.getProperty("B,$bedrockId,$bedrockData") ?: "$bedrockId,$bedrockData"
            ).split(',', limit = 2)
    if (nukkitId != "0") {
        sequenceOf(javaBlock to (nukkitBlockNames[nukkitId.toInt()] ?: listOf(nukkitId, javaBlock)))
    } else {
        sequenceOf(null)
    }
}.filterNotNull()

internal val tippedArrows = propertiesStringInt("/tipped-arrows.properties")

internal data class PaintingData(val id: String, val width: Int, val height: Int)
internal val paintings = propertiesStringString("/paintings.properties")
    .mapValues { (_, value) ->
        val (id, width, height) = value.split(',', limit = 3)
        PaintingData(id, width.toInt(), height.toInt())
    }
internal val javaEntities2Nukkit = propertiesStringString("/java-entities.properties")

internal val javaEnchantments2Nukkit = propertiesStringInt("/enchantments.properties")

internal val javaPotions2Bedrock = propertiesStringInt("/potions.properties")
