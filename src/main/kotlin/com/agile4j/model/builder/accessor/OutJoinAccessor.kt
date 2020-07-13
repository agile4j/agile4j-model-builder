package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class OutJoinAccessor<A : Any, AI, OJ>(private val outJoinPoint: String) : IAccessor<A, OJ> {
    @Suppress("UNCHECKED_CAST")
    override fun get(sources: Collection<A>): Map<A, OJ> {
        val modelBuilder = ITargetDelegate.ScopeKeys.modelBuilderScopeKey.get()!!

        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val outJoinPointToMapperMap = BuildContext.outJoinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) return emptyMap()

        val indexer = BuildContext.indexerHolder[accompanies.elementAt(0)::class] as (A) -> AI
        val accompanyToAccompanyIndexMap : Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToAccompanyMap = accompanyToAccompanyIndexMap.map { (k, v) -> v to k }.toMap()

        /*val accompanyIndices = accompanyToAccompanyIndexMap.values.stream()
            .filter(Objects::nonNull).collect(Collectors.toSet())*/
        val cacheMap = modelBuilder.outJoinCacheMap.computeIfAbsent(outJoinPoint) { mutableMapOf()} as MutableMap<A, OJ>
        val cached = cacheMap.filterKeys { accompanies.contains(it) }
        val accompanyIndexToOutJoinCached = cached.mapKeys { accompanyToAccompanyIndexMap[it.key] ?: error("43423") }
        val unCachedAccompanies = accompanies.filter { !cached.keys.contains(it) }
        val unCachedAccompanyIndices = unCachedAccompanies
            .map { accompanyToAccompanyIndexMap[it] ?: error("4534") }.toSet()

        val mapper = outJoinPointToMapperMap!![outJoinPoint] as (Collection<AI>) -> Map<AI, OJ>
        val buildAccompanyIndexToOutJoinMap = mapper.invoke(unCachedAccompanyIndices)

        val buildAccompanyToOutJoinMap = buildAccompanyIndexToOutJoinMap.mapKeys { accompanyIndexToAccompanyMap[it.key] ?: error("5534") }

        cacheMap.putAll(buildAccompanyToOutJoinMap) // 入缓存

        val valueMap = mutableMapOf<AI, OJ>()
        valueMap.putAll(accompanyIndexToOutJoinCached)
        valueMap.putAll(buildAccompanyIndexToOutJoinMap)

        return accompanyToAccompanyIndexMap.mapValues { valueMap[it.value] ?: error("") }
    }
}