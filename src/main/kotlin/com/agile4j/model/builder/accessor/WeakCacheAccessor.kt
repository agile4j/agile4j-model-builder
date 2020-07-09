package com.agile4j.model.builder.accessor

import com.agile4j.utils.access.IAccessor
import java.util.concurrent.ConcurrentHashMap

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

abstract class WeakCacheAccessor<K, V> : IAccessor<K, V> {
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