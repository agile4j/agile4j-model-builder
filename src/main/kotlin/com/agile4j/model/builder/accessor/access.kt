package com.agile4j.model.builder.accessor

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author liurenpeng
 * Created on 2020-06-17
 */

interface IAccessor<K, V> {
    fun get(sources: Collection<K>): Map<K, V>
    fun set(dataMap: Map<K, V>) {}
}

abstract class CacheAccessor<K, V> : IAccessor<K, V> {
    private val cache = ConcurrentHashMap<K, V>()

    override fun get(sources: Collection<K>): Map<K, V> {
        val cached = cache.filter { sources.contains(it.key) }
        val unCachedKeys = sources.filter { !cached.keys.contains(it) }
        return cached + realGet(unCachedKeys)
    }

    override fun set(dataMap: Map<K, V>) {
        cache.putAll(dataMap)
    }

    abstract fun realGet(sources: Collection<K>): Map<K, V>
}


fun <K, V> access(keys: Collection<K>, list: Iterable<IAccessor<K, V>>): Map<K, V> {
    return accessByIterator(keys.toSet(), list.iterator())
}

private fun <K, V> accessByIterator(keys: Collection<K>, iterator: Iterator<IAccessor<K, V>>): Map<K, V> {
    if (!iterator.hasNext() || keys.isEmpty()) {
        return emptyMap()
    }

    val currentAccessor = iterator.next()
    val originalResult = currentAccessor.get(keys)
    val leftKeys = HashSet<K>()
    val result = mutableMapOf<K, V>()
    for (key in keys) {
        val value = originalResult[key]
        if (value == null) {
            leftKeys.add(key)
        } else {
            result[key] = value
        }
    }

    val lowerResult = accessByIterator(leftKeys, iterator)
    if (lowerResult.isNotEmpty()) {
        result.putAll(lowerResult)
    }
    currentAccessor.set(result)
    return result
}