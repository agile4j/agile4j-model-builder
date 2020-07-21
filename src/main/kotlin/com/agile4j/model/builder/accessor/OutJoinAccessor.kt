package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.utils.reverseKV
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil

/**
 * @param A accompany
 * @param AI accompanyIndex
 * @param OJM outJoinModel
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class OutJoinAccessor<A : Any, AI, OJM>(
    private val outJoinPoint: String
) : BaseAccessor<A, OJM>() {

    override fun get(accompanies: Collection<A>): Map<A, OJM> {
        val mapper = getMapper(accompanies)

        val accompanyClazz = accompanies.elementAt(0)::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> AI
        val accompanyToAccompanyIndexMap : Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToAccompanyMap = accompanyToAccompanyIndexMap.reverseKV()

        val allCacheMap = modelBuilder.getOutJoinCacheMap(outJoinPoint) as Map<A, OJM>
        val cached = allCacheMap.filterKeys { accompanies.contains(it) }
        val unCachedAccompanies = accompanies.filter { !cached.keys.contains(it) }
        val unCachedAccompanyIndices = unCachedAccompanies
            .map { accompanyToAccompanyIndexMap[it]
                ?: throw ModelBuildException("not found matched index. accompany:$it") }.toSet()

        val accompanyToOutJoinMap = mutableMapOf<A, OJM>()
        accompanyToOutJoinMap.putAll(cached)
        if (CollectionUtil.isNotEmpty(unCachedAccompanyIndices)) {
            val buildAccompanyIndexToOutJoinMap = mapper.invoke(unCachedAccompanyIndices)
            val buildAccompanyToOutJoinMap = buildAccompanyIndexToOutJoinMap
                .mapKeys { accompanyIndexToAccompanyMap[it.key]
                    ?: throw ModelBuildException("not found matched accompany. index:${it.key}") }
            modelBuilder.putAllOutJoinCacheMap(outJoinPoint, buildAccompanyToOutJoinMap) // 入缓存
            accompanyToOutJoinMap.putAll(buildAccompanyToOutJoinMap)
        }
        return accompanyToOutJoinMap
    }

    private fun getMapper(accompanies: Collection<A>): (Collection<AI>) -> Map<AI, OJM> {
        if (CollectionUtil.isEmpty(accompanies)) throw ModelBuildException("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as MutableMap<String, (Collection<AI>) -> Map<AI, OJM>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) throw ModelBuildException("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinPoint]
            ?: throw ModelBuildException("not found matched mapper. outJoinPoint:$outJoinPoint")
    }
}