package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class OutJoinTargetAccessor<A : Any, AI, OJT>(private val outJoinTargetPoint: String) : IAccessor<A, OJT> {
    @Suppress("UNCHECKED_CAST")
    override fun get(sources: Collection<A>): Map<A, OJT> {
        val modelBuilder = modelBuilderScopeKey.get()!!


        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val outJoinAccompanyPointToMapperMap = BuildContext.outJoinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(outJoinAccompanyPointToMapperMap)) return emptyMap()
        val mapper = outJoinAccompanyPointToMapperMap!![outJoinTargetPoint] as (Collection<AI>) -> Map<AI, Any>

        val indexer = BuildContext.indexerHolder[accompanies.elementAt(0)::class] as (A) -> AI
        val accompanyToAccompanyIndexMap: Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val accompanyIndexToAccompanyMap = accompanyToAccompanyIndexMap.map { (k, v) -> v to k }.toMap()

        val accompanyIndices = accompanyToAccompanyIndexMap.values
        val cacheMap = modelBuilder.outJoinTargetCacheMap
            .computeIfAbsent(outJoinTargetPoint) { mutableMapOf()} as MutableMap<A, OJT>
        val cached = cacheMap.filterKeys { accompanies.contains(it) }
        val accompanyIndexToOutJoinCached = cached.mapKeys { accompanyToAccompanyIndexMap[it.key] ?: error("43423") }
        val unCachedAccompanies = accompanies.filter { !cached.keys.contains(it) }
        val unCachedKeys = unCachedAccompanies.map { accompanyToAccompanyIndexMap[it] ?: error("3423") }


        val buildAccompanyIndexToOutJoinAccompanyMap = mapper.invoke(unCachedKeys)

        val allAccompanyIndexToOutJoinAccompanyMap = mutableMapOf<AI, Any>()
        allAccompanyIndexToOutJoinAccompanyMap.putAll(buildAccompanyIndexToOutJoinAccompanyMap)
        allAccompanyIndexToOutJoinAccompanyMap.putAll(accompanyIndexToOutJoinCached.mapValues { modelBuilder.targetToAccompanyMap[it]!! })

        val allAccompanyToOutJoinAccompanyMap = allAccompanyIndexToOutJoinAccompanyMap
            .mapKeys { accompanyIndexToAccompanyMap[it.key] ?: error("67455") }

        val isCollection = Collection::class.java.isAssignableFrom(
            allAccompanyIndexToOutJoinAccompanyMap.values.elementAt(0)::class.java
        )
        val outJoinAccompanyClazz = if (!isCollection) {
            allAccompanyIndexToOutJoinAccompanyMap.values.elementAt(0)::class
        } else {
            val coll = allAccompanyIndexToOutJoinAccompanyMap.values.elementAt(0) as Collection<Any>
            coll.elementAt(0)::class
        }

        val accompanyToTargetMap = BuildContext.accompanyHolder.map { (k, v) -> v to k }.toMap()
        val outJoinTargetClazz = accompanyToTargetMap[outJoinAccompanyClazz] as KClass<Any>

        val outJoinAccompanies = if (!isCollection) {
            buildAccompanyIndexToOutJoinAccompanyMap.values
        } else {
            buildAccompanyIndexToOutJoinAccompanyMap.values.stream().flatMap {
                it as Collection<Any>
                it.stream()
            }.collect(Collectors.toList())
        }
        val outJoinTargets = modelBuilder buildMulti outJoinTargetClazz by outJoinAccompanies
        //val outJoinTargets = ModelBuilder() buildMulti outJoinTargetClazz by outJoinAccompanies

        cacheMap.putAll(outJoinTargets.map { (
                if (!isCollection) {
                    allAccompanyToOutJoinAccompanyMap.filter { e ->  e.value == modelBuilder.targetToAccompanyMap[it]}.entries
                        .stream().findFirst().map { e -> e.key }.orElseThrow { ModelBuildException("") }
                } else {
                    allAccompanyToOutJoinAccompanyMap.filter { e ->
                        val ojas = e.value as Collection<Any>
                        val ojts = it as Collection<Any>
                        ojts.stream().allMatch { ojt -> ojas.contains(modelBuilder.targetToAccompanyMap[ojt]) }
                    }.entries
                        .stream().findFirst().map { e -> e.key }.orElseThrow { ModelBuildException("") }
                }
                ) to it as OJT}.toMap()) // 入缓存


        return accompanyToAccompanyIndexMap.mapValues { (_, accompanyIndex) ->
            val outJoinAccompany = buildAccompanyIndexToOutJoinAccompanyMap[accompanyIndex]
            val target = if (!isCollection) {
                outJoinTargets.first { outJoinTarget ->
                    outJoinTarget.buildInModelBuilder
                        .targetToAccompanyMap[outJoinTarget] == outJoinAccompany
                }
            } else {
                outJoinTargets.filter { outJoinTarget ->
                    outJoinAccompany as Collection<Any>
                    outJoinAccompany.contains(outJoinTarget.buildInModelBuilder
                        .targetToAccompanyMap[outJoinTarget])
                }
            }
            target
        } as Map<A, OJT>
    }
}