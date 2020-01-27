package br.com.gamemods.j2nwc.internal

import java.util.*

internal fun properties(name: String) = Properties().apply {
    JavaPalette::class.java.getResourceAsStream(name).bufferedReader().use {
        load(it)
    }
}

private fun propertiesStringString(name: String) = properties(name)
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString() }

internal fun propertiesStringInt(name: String) = properties(name)
    .mapKeys { it.key.toString().toLowerCase() }.mapValues { it.value.toString().toInt() }

internal val java2bedrockEntities = propertiesStringInt("/entity-ids.properties")

internal val java2bedrockStates = propertiesStringString("/block-states.properties").entries.asSequence().flatMap { e->
    if (";waterlogged-false" in e.key) {
        sequenceOf(e.toPair(), e.key.replace(";waterlogged-false", "") to e.value)
    } else {
        sequenceOf(e.toPair())
    }
}.flatMap { e ->
    if (";powered-false" in e.first && e.first.matches(Regex("^[^_]+_trapdoor;.*"))) {
        sequenceOf(e, e.first.replace(";powered-false", "") to e.second)
    } else {
        sequenceOf(e)
    }
}.toMap()

internal val java2bedrockItems = propertiesStringString("/items.properties")

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

internal val javaBiomes2Bedrock = properties("/biomes.properties").asSequence()
    .map { it.key.toString().substringBefore('-').toInt() to it.value.toString().toInt().toByte() }
    .toMap()

internal val javaBiomesString2Bedrock = properties("/biomes.properties").asSequence()
    .map { it.key.toString().substringAfter('-') to it.value.toString().toInt().toByte() }
    .toMap()

internal val javaInheritedWaterlogging = JavaPalette::class.java.getResourceAsStream("/java-inherited-waterlogging.txt").bufferedReader().use {
    it.readLines()
}.toSet()
