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
internal class OutJoinTargetAccessor<A : Any, AI : Any, OJA : Any, OJT : Any, OJARM : Any, OJTRM : Any>(
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
        val aToAi = accompanies.map { it to indexer.invoke(it) }.toMap()
        val aiToA = aToAi.reverseKV()
        val unCachedAi = unCachedA.map { a -> aToAi[a]!! }

        val mapper = getMapper(accompanies)
        val buildAiToOjarmMap = mapper.invoke(unCachedAi) // call biz sys
        val buildAToOjarmMap = buildAiToOjarmMap.mapKeys { aiToA[it.key]!! }
        if (MapUtil.isEmpty(buildAToOjarmMap)) return cached // all cached

        val ojarm = buildAToOjarmMap.firstValue()!!
        val isCollection = ojarm is Collection<*>
        val ojaClazz = getOjaClazz(ojarm)
        val ojtClazz = BuildContext.accompanyHolder.reverseKV()[ojaClazz] as KClass<OJT>

        val ojas = getOjas(isCollection, buildAToOjarmMap)
        modelBuilder buildMulti ojtClazz by ojas
        val ojaToOjtMap = modelBuilder.accompanyToTargetMap

        val buildAToOjtrmMap = getAToOjtrmMap(isCollection, buildAToOjarmMap, ojaToOjtMap)
        modelBuilder.putAllOutJoinTargetCacheMap(outJoinTargetPoint, buildAToOjtrmMap) // 入缓存
        return cached + buildAToOjtrmMap
    }

    private fun getAToOjtrmMap(
        isCollection: Boolean,
        buildAToOjarmMap: Map<A, OJARM>,
        ojaToOjtMap: Map<Any, Any>
    ): Map<A, OJTRM> =
        if (!isCollection) {
            buildAToOjarmMap.mapValues { v -> ojaToOjtMap[v] }
        } else {
            buildAToOjarmMap.mapValues { e ->
                val collValue = e.value as Collection<Any>
                collValue.map { v -> ojaToOjtMap[v] }
            }
        } as Map<A, OJTRM>

    private fun getOjas(
        isCollection: Boolean,
        buildAToOJARMMap: Map<A, OJARM>
    ): Collection<OJA> =
        if (!isCollection) {
            buildAToOJARMMap.values as Collection<OJA>
        } else {
            buildAToOJARMMap.values.stream().flatMap {
                (it as Collection<OJA>).stream()
            }.collect(Collectors.toList()) as Collection<OJA>
        }

    private fun getOjaClazz(
        ojarm: OJARM
    ): KClass<OJA> =
        if (ojarm !is Collection<*>) {
            ojarm::class
        } else {
            (ojarm as Collection<Any>).first()::class
        } as KClass<OJA>

    private fun getMapper(
        accompanies: Set<A>
    ): (Collection<AI>) -> Map<AI, OJARM> {
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