package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.delegate.map.WeakIdentityHashMap
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class JoinAccessor<A: Any, JI, J>(private val joinClazz: KClass<*>) : IAccessor<A, Map<JI, J>> {
    @Suppress("UNCHECKED_CAST")
    override fun get(sources: Collection<A>): Map<A, Map<JI, J>> {
        val modelBuilder = modelBuilderScopeKey.get()!!

        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val joinClazzToMapperMap = BuildContext.joinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(joinClazzToMapperMap)) return emptyMap()
        val mapper = joinClazzToMapperMap!![joinClazz] as MutableList<(A) -> JI>

        val targetToJoinIndices : Map<A, Set<JI>> = accompanies.map { it to
                mapper.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val indices = targetToJoinIndices.values.stream().flatMap { it.stream() }.toList()
        val cacheMap = modelBuilder.joinCacheMap.computeIfAbsent(joinClazz) { WeakIdentityHashMap() } as MutableMap<JI, J>
        val cached = cacheMap.filterKeys { indices.contains(it) }
        val unCachedKeys = indices.filter { !cached.keys.contains(it) }

        val valueMap = mutableMapOf<JI, J>()
        valueMap.putAll(cached)

        if (CollectionUtil.isNotEmpty(unCachedKeys)) {
            val builder = BuildContext.builderHolder[joinClazz] as (Collection<JI>) -> Map<JI, J>
            val buildValueMap = builder.invoke(unCachedKeys)
            cacheMap.putAll(buildValueMap) // 入缓存
            valueMap.putAll(buildValueMap)
        }

        return targetToJoinIndices.mapValues { i -> valueMap.filter { i.value.contains(it.key) } }
    }
}