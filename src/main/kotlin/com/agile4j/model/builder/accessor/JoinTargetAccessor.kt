package com.agile4j.model.builder.accessor

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
 * JTAI:JoinTargetAccompanyIndex
 * @author liurenpeng
 * Created on 2020-06-18
 */
class JoinTargetAccessor<A: Any, JTAI, JT: Any>(private val joinTargetClazz: KClass<Any>) : IAccessor<A, Map<JTAI, JT>> {
    @Suppress("UNCHECKED_CAST")
    override fun get(sources: Collection<A>): Map<A, Map<JTAI, JT>> {
        val modelBuilder = modelBuilderScopeKey.get()!!

        val accompanies = sources.toSet()
        if (CollectionUtil.isEmpty(accompanies)) return emptyMap()
        val joinAccompanyClazzToMapperMap = BuildContext.joinHolder[accompanies.elementAt(0)::class]
        if (MapUtil.isEmpty(joinAccompanyClazzToMapperMap)) return emptyMap()
        val joinAccompanyClazz = BuildContext.accompanyHolder[joinTargetClazz]
        val mappers = joinAccompanyClazzToMapperMap!![joinAccompanyClazz] as MutableList<(A) -> JTAI>

        val accompanyIndexer = BuildContext.indexerHolder[accompanies.elementAt(0)::class] as (A) -> Any
        val indexToAccompanyMap = accompanies.map {accompanyIndexer.invoke(it) to it}.toMap()
        val accompanyToIndexMap = indexToAccompanyMap.map { (k, v) -> v to k }.toMap()
        val accompanyIndices = indexToAccompanyMap.keys


        val allAccompanyToJoinTargetAccompanyIndices : Map<A, Set<JTAI>> = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val allJoinTargetAccompanyIndices = allAccompanyToJoinTargetAccompanyIndices.values.stream()
            .flatMap { it.stream() }.collect(Collectors.toSet())


        val cacheMap = modelBuilder.joinTargetCacheMap
            .computeIfAbsent(joinTargetClazz) { mutableMapOf()} as MutableMap<Any, JT>
        val filteredCached = cacheMap.filterKeys { allJoinTargetAccompanyIndices.contains(it as JTAI) }
        val unCachedKeys = accompanyIndices.filter { !filteredCached.keys.contains(it) }
        val unCachedAccompanies = accompanies.filter { unCachedKeys.contains(accompanyToIndexMap[it]) }

        val unCachedAccompanyToJoinTargetAccompanyIndices : Map<A, Set<JTAI>> = unCachedAccompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        /*val allAccompanyToJoinTargetAccompanyIndices = mutableMapOf<A, Set<JTAI>>()
        allAccompanyToJoinTargetAccompanyIndices.putAll(unCachedAccompanyToJoinTargetAccompanyIndices)
        allAccompanyToJoinTargetAccompanyIndices.putAll(filteredCached.keys
            .map { (indexToAccompanyMap[it] ?: error("3443")) to
                mappers.map { mapper -> (mapper.invoke((indexToAccompanyMap[it] ?: error("3443")))) }.toSet()}.toMap())*/

        val unCachedJoinTargetAccompanyIndices = unCachedAccompanyToJoinTargetAccompanyIndices.values.stream()
            .flatMap{it.stream()}.collect(Collectors.toSet())

        val allTargets = mutableListOf<JT>()
        allTargets.addAll(filteredCached.values)

        if (CollectionUtil.isNotEmpty(unCachedJoinTargetAccompanyIndices)) {
            // TODO 弄个新的ModelBuilder()并且把老的cache merge过去，另外把当前target\accompany，也merge进cache
            val buildTargetsTemp = modelBuilder buildMulti joinTargetClazz by unCachedJoinTargetAccompanyIndices
            val buildTargets = buildTargetsTemp as Collection<JT>

            //println("+++${modelBuilder.joinTargetCacheMap}")
            /*val needCacheMap = buildTargets.map {
                unCachedAccompanyToJoinTargetAccompanyIndices
                    .filter { e ->  e.value.contains(modelBuilder.targetToIndexMap[it] as JTAI) }
                    .entries.stream().findFirst().map { e -> accompanyToIndexMap[e.key] ?: error("5454") }
                    .orElseThrow { ModelBuildException("123") } to it}.toMap()*/
            val needCacheMap = buildTargets.map { modelBuilder.targetToIndexMap[it]!! to it}.toMap()
            cacheMap.putAll(needCacheMap) // 入缓存
            //println("+++${modelBuilder.joinTargetCacheMap}")


            allTargets.addAll(buildTargets)
        }

        /*val targets = ModelBuilder() buildMulti joinTargetClazz by (accompanyToJoinTargetAccompanyIndices.values
            .stream().flatMap { it.stream() }.collect(Collectors.toSet()) as Set<JTAI>)*/
        val result = allAccompanyToJoinTargetAccompanyIndices.mapValues { (_, joinTargetAccompanyIndices) ->
            val currTargets = allTargets.filter { joinTargetAccompanyIndices
                .contains(it.buildInModelBuilder.accompanyToIndexMap[
                        it.buildInModelBuilder.targetToAccompanyMap[it]]) }.toList()
            currTargets.map { target -> parseTargetToAccompanyIndex(target) to target }.toMap()
        } as Map<A, Map<JTAI, JT>>

        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun parseTargetToAccompanyIndex(target : Any) : Any {
        val modelBuilder = target.buildInModelBuilder
        val accompanyToIndexMap = modelBuilder.indexToAccompanyMap.map { (k, v) -> v to k}.toMap()
        return modelBuilder.targetToAccompanyMap.mapValues { accompanyToIndexMap[it.value] }[target] ?: error("")
    }
}