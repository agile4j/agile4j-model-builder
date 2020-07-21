package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
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
 * @param A accompany
 * @param AI accompanyIndex
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class OutJoinTargetAccessor<A : Any, AI>(private val outJoinTargetPoint: String) : IAccessor<A, Any> {

    override fun get(sources: Collection<A>): Map<A, Any> {
        val modelBuilder = modelBuilderScopeKey.get() ?: err("modelBuilderScopeKey not init")
        val accompanies = sources.toSet()
        val mapper = getMapper(accompanies)

        val accompanyClazz = accompanies.elementAt(0)::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> AI
        val accompanyToAccompanyIndexMap: Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToAccompanyMap = accompanyToAccompanyIndexMap.reverseKV()

        // allCacheMap类型为May<A,Any>, Any可能为OJT(OutJoinTarget)也可能为Collection<OJT>
        val allCacheMap = modelBuilder.getOutJoinTargetCacheMap(outJoinTargetPoint) as Map<A, Any>
        val cached = allCacheMap.filterKeys { accompanies.contains(it) }
        val unCachedAccompanies = accompanies.filter { !cached.keys.contains(it) }
        val unCachedAccompanyIndices = unCachedAccompanies.map {
            accompanyToAccompanyIndexMap[it] ?: err("not found matched index. accompany:$it") }

        if (CollectionUtil.isEmpty(unCachedAccompanyIndices)) {
            return cached
        }

        // 全称为allAccompanyToOutJoinTargetRelatedModelMap，太长了简写下
        // 叫做TargetRelatedModel是因为其类型Any可能为OJT(OutJoinTarget)也可能为Collection<OJT>
        val allAToOJTRMMap = mutableMapOf<A, Any>()
        allAToOJTRMMap.putAll(cached)

        // 全称为：buildAccompanyIndexToOutJoinAccompanyRelatedModelMap，太长了简写下
        // 叫做AccompanyRelatedModel是因为其类型Any可能为OJA(OutJoinAccompany)也可能为Collection<OJA>
        val buildAIToOJARMMap = mapper.invoke(unCachedAccompanyIndices)
        val buildAToOJARMMap = buildAIToOJARMMap.mapKeys {
            accompanyIndexToAccompanyMap[it.key] ?: err("not found matched accompany. index:${it.key}") }

        val accompanyClazzToTargetClazzMap = BuildContext.accompanyHolder.reverseKV()

        val isCollection: Boolean
        lateinit var outJoinTargetClazz: KClass<Any>
        when {
            MapUtil.isNotEmpty(buildAToOJARMMap) -> {
                // ojarm: outJoinAccompanyRelatedModel
                val ojarm = buildAToOJARMMap.firstValue() ?: err("found null value. buildAToOJARMMap:$buildAToOJARMMap")
                isCollection = Collection::class.java.isAssignableFrom(ojarm::class.java)
                val outJoinAccompanyClazz =
                    if (!isCollection) { ojarm::class } else { (ojarm as Collection<Any>).first()::class }
                outJoinTargetClazz = accompanyClazzToTargetClazzMap[outJoinAccompanyClazz] as KClass<Any>
            }
            MapUtil.isNotEmpty(cached) -> {
                val ojtrm = cached.firstValue() ?: err("found null value. cached:$cached")
                isCollection = Collection::class.java.isAssignableFrom(ojtrm::class.java)
                outJoinTargetClazz = if (!isCollection) { ojtrm::class } else {
                    (ojtrm as Collection<Any>).first()::class } as KClass<Any>
            }
            else -> return emptyMap()
        }

        val outJoinAccompanies = if (!isCollection) {
            buildAIToOJARMMap.values
        } else {
            buildAIToOJARMMap.values.stream().flatMap {
                it as Collection<Any>
                it.stream()
            }.collect(Collectors.toList())
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
        }
        modelBuilder.putAllOutJoinTargetCacheMap(outJoinTargetPoint, buildAToOJTRMMap) // 入缓存
        allAToOJTRMMap.putAll(buildAToOJTRMMap)

        return allAToOJTRMMap
    }

    private fun getMapper(accompanies: Set<A>): (Collection<AI>) -> Map<AI, Any> {
        if (CollectionUtil.isEmpty(accompanies)) throw ModelBuildException("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as Map<String, (Collection<AI>) -> Map<AI, Any>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) throw ModelBuildException("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinTargetPoint]
            ?: throw ModelBuildException("not found matched mapper. outJoinTargetPoint:$outJoinTargetPoint")
    }
}