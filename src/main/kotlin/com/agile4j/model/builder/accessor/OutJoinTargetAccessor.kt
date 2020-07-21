package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.utils.firstValue
import com.agile4j.model.builder.utils.reverseKV
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 *
 * abbreviations:
 * A        accompany
 * AI       accompanyIndex
 * OJA      outJoinAccompany
 * OJT      outJoinTarget
 * OJARM    outJoinAccompanyRelatedModel: OJA or Collection<OJA>
 * OJTRM    outJoinTargetRelatedModel: OJT or Collection<OJT>
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
internal class OutJoinTargetAccessor<A: Any, AI: Any, OJA: Any, OJT: Any, OJARM: Any, OJTRM: Any>(
    private val outJoinTargetPoint: String
) : IAccessor<A, OJTRM> {

    override fun get(sources: Collection<A>): Map<A, OJTRM> {
        val modelBuilder = modelBuilderScopeKey.get()!!
        val accompanies = sources.toSet()

        val allCacheMap = modelBuilder.getOutJoinTargetCacheMap(outJoinTargetPoint) as Map<A, OJTRM>
        val cached = allCacheMap.filterKeys { accompanies.contains(it) }
        val unCachedA = accompanies.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedA)) return cached // all cached

        val accompanyClazz = accompanies.first()::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> AI
        val aToAI = accompanies.map { it to indexer.invoke(it) }.toMap()
        val aiToA = aToAI.reverseKV()
        val unCachedAI = unCachedA.map { a -> aToAI[a]!! }

        val mapper = getMapper(accompanies)
        val buildAIToOJARMMap = mapper.invoke(unCachedAI) // call biz sys
        val buildAToOJARMMap = buildAIToOJARMMap.mapKeys { aiToA[it.key]!! }
        if (MapUtil.isEmpty(buildAToOJARMMap)) return cached // all cached

        val ojarm = buildAToOJARMMap.firstValue()!!
        val isCollection = ojarm is Collection<*>
        val ojaClazz = getOJAClazz(ojarm)
        val ojtClazz = BuildContext.accompanyHolder.reverseKV()[ojaClazz] as KClass<OJT>

        val outJoinAccompanies = getOJAs(buildAToOJARMMap, isCollection)
        modelBuilder buildMulti ojtClazz by outJoinAccompanies
        val outJoinAccompanyToOutJoinTargetMap = modelBuilder.accompanyToTargetMap

        val buildAToOJTRMMap = if (!isCollection) {
            buildAToOJARMMap.mapValues { v -> outJoinAccompanyToOutJoinTargetMap[v] ?: err("423") }
        } else {
            buildAToOJARMMap.mapValues { e ->
                val collValue = e.value as Collection<Any>
                collValue.map { v -> outJoinAccompanyToOutJoinTargetMap[v] ?: err("423") }
            }
        } as Map<A, OJTRM>
        modelBuilder.putAllOutJoinTargetCacheMap(outJoinTargetPoint, buildAToOJTRMMap) // 入缓存

        return cached + buildAToOJTRMMap
    }

    private fun getOJAs(buildAToOJARMMap: Map<A, OJARM>, isCollection: Boolean) =
        if (!isCollection) {
            buildAToOJARMMap.values as Collection<OJA>
        } else {
            buildAToOJARMMap.values.stream().flatMap {
                (it as Collection<OJA>).stream()
            }.collect(Collectors.toList()) as Collection<OJA>
        }

    private fun getOJAClazz(ojarm: OJARM): KClass<OJA> =
        if (ojarm !is Collection<*>) { ojarm::class }
        else { (ojarm as Collection<Any>).first()::class } as KClass<OJA>

    private fun getMapper(accompanies: Set<A>): (Collection<AI>) -> Map<AI, OJARM> {
        if (CollectionUtil.isEmpty(accompanies)) err("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as Map<String, (Collection<AI>) -> Map<AI, OJARM>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) err("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinTargetPoint]
            ?: err("not found matched mapper. outJoinTargetPoint:$outJoinTargetPoint")
    }

    companion object {
        fun outJoinTargetAccessor(outJoinTargetPoint: String) =
            OutJoinTargetAccessor<Any, Any, Any, Any, Any, Any>(outJoinTargetPoint)
    }
}