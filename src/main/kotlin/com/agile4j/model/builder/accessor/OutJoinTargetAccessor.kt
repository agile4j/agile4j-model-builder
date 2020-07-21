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
        val mapper = getMapper(accompanies)

        // allCacheMap类型为May<A,Any>, Any可能为OJT(OutJoinTarget)也可能为Collection<OJT>
        val allCacheMap = modelBuilder.getOutJoinTargetCacheMap(outJoinTargetPoint) as Map<A, OJTRM>
        val cached = allCacheMap.filterKeys { accompanies.contains(it) }
        val unCachedAccompanies = accompanies.filter { !cached.keys.contains(it) }

        if (CollectionUtil.isEmpty(unCachedAccompanies)) {
            return cached
        }

        val accompanyClazz = accompanies.first()::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> AI
        val aToAI: Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val unCachedAccompanyIndices = unCachedAccompanies.map {
            aToAI[it] ?: err("not found matched index. accompany:$it") }

        // 全称为allAccompanyToOutJoinTargetRelatedModelMap，太长了简写下
        // 叫做TargetRelatedModel是因为其类型Any可能为OJT(OutJoinTarget)也可能为Collection<OJT>
        val allAToOJTRMMap = mutableMapOf<A, OJTRM>()
        allAToOJTRMMap.putAll(cached)

        // 全称为：buildAccompanyIndexToOutJoinAccompanyRelatedModelMap，太长了简写下
        // 叫做AccompanyRelatedModel是因为其类型Any可能为OJA(OutJoinAccompany)也可能为Collection<OJA>
        val buildAIToOJARMMap = mapper.invoke(unCachedAccompanyIndices)
        val buildAToOJARMMap = buildAIToOJARMMap.mapKeys {
            aToAI.reverseKV()[it.key] ?: err("not found matched accompany. index:${it.key}") }

        val accompanyClazzToTargetClazzMap = BuildContext.accompanyHolder.reverseKV()

        val isCollection: Boolean
        lateinit var outJoinTargetClazz: KClass<OJT>
        when {
            MapUtil.isNotEmpty(buildAToOJARMMap) -> {
                // ojarm: outJoinAccompanyRelatedModel
                val ojarm = buildAToOJARMMap.firstValue() ?: err("found null value. buildAToOJARMMap:$buildAToOJARMMap")
                isCollection = Collection::class.java.isAssignableFrom(ojarm::class.java)
                val outJoinAccompanyClazz =
                    if (!isCollection) { ojarm::class } else { (ojarm as Collection<Any>).first()::class }
                outJoinTargetClazz = accompanyClazzToTargetClazzMap[outJoinAccompanyClazz] as KClass<OJT>
            }
            MapUtil.isNotEmpty(cached) -> {
                val ojtrm = cached.firstValue() ?: err("found null value. cached:$cached")
                isCollection = Collection::class.java.isAssignableFrom(ojtrm::class.java)
                outJoinTargetClazz = if (!isCollection) { ojtrm::class } else {
                    (ojtrm as Collection<Any>).first()::class } as KClass<OJT>
            }
            else -> return emptyMap()
        }

        val outJoinAccompanies = if (!isCollection) {
            buildAIToOJARMMap.values as Collection<OJA>
        } else {
            buildAIToOJARMMap.values.stream().flatMap {
                (it as Collection<OJA>).stream()
            }.collect(Collectors.toList()) as Collection<OJA>
        }
        modelBuilder buildMulti outJoinTargetClazz by outJoinAccompanies
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
        allAToOJTRMMap.putAll(buildAToOJTRMMap)

        return allAToOJTRMMap
    }

    private fun getMapper(accompanies: Set<A>): (Collection<AI>) -> Map<AI, OJARM> {
        if (CollectionUtil.isEmpty(accompanies)) err("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as Map<String, (Collection<AI>) -> Map<AI, OJARM>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) err("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinTargetPoint]
            ?: err("not found matched mapper. outJoinTargetPoint:$outJoinTargetPoint")
    }
}