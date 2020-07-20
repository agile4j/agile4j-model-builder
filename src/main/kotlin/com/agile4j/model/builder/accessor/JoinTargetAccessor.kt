package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * @param JI joinIndex
 * @param JT joinTarget
 * @author liurenpeng
 * Created on 2020-06-18
 */
class JoinTargetAccessor<A: Any, JI, JT: Any>(private val joinTargetClazz: KClass<Any>) :
    IAccessor<A, Map<JI, JT>> {
    @Suppress("UNCHECKED_CAST")
    override fun get(sources: Collection<A>): Map<A, Map<JI, JT>> {
        val modelBuilder = modelBuilderScopeKey.get()
            ?: throw ModelBuildException("modelBuilderScopeKey not init")
        val accompanies = sources.toSet()
        val mappers = getMappers(accompanies)
        if (CollectionUtil.isEmpty(mappers)) return emptyMap()

        val allAccompanyToJoinIndices = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val allJoinIndices = allAccompanyToJoinIndices.values.stream()
            .flatMap { it.stream() }.collect(Collectors.toSet())

        val allCacheMap = modelBuilder.getJoinTargetCacheMap(joinTargetClazz) as Map<JI, JT>
        val cached = allCacheMap.filterKeys { allJoinIndices.contains(it) }
        val unCachedJoinIndices = allJoinIndices.filter { !cached.keys.contains(it) }

        val allTargets = mutableListOf<JT>()
        allTargets.addAll(cached.values)

        if (CollectionUtil.isNotEmpty(unCachedJoinIndices)) {
            // TODO 弄个新的ModelBuilder()并且把老的cache merge过去，另外把当前target\accompany，也merge进cache
            val buildTargetsTemp = modelBuilder buildMulti joinTargetClazz by unCachedJoinIndices
            //modelBuilder.indext
            // TODO


            val buildTargets = buildTargetsTemp as Collection<JT>

            val needCacheMap = buildTargets.map { modelBuilder.targetToIndexMap[it]!! to it}.toMap()
            modelBuilder.putAllJoinTargetCacheMap(joinTargetClazz, needCacheMap)  // 入缓存
            //reverseCacheMap.putAll(needCacheMap.map { (k, v) -> v to k }.toMap())

            allTargets.addAll(buildTargets)
        }

        return allAccompanyToJoinIndices.mapValues { (_, joinIndices) ->
            val currTargets = allTargets.filter { joinIndices
                .contains(it.buildInModelBuilder.accompanyToIndexMap[
                        it.buildInModelBuilder.targetToAccompanyMap[it]]) }.toList()
            currTargets.map { target -> parseTargetToAccompanyIndex(target) to target }.toMap()
        } as Map<A, Map<JI, JT>>
    }

    @Suppress("UNCHECKED_CAST")
    fun parseTargetToAccompanyIndex(target : Any) : Any {
        val modelBuilder = target.buildInModelBuilder
        val accompanyToIndexMap = modelBuilder.indexToAccompanyMap.map { (k, v) -> v to k}.toMap()
        return modelBuilder.targetToAccompanyMap.mapValues { accompanyToIndexMap[it.value] }[target] ?: error("")
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMappers(accompanies: Set<A>): List<(A) -> JI> {
        if (CollectionUtil.isEmpty(accompanies)) return emptyList()
        val accompanyClazz = accompanies.elementAt(0)::class
        val joinAccompanyClazzToMapperMap = BuildContext.joinHolder[accompanyClazz]
        if (MapUtil.isEmpty(joinAccompanyClazzToMapperMap)) return emptyList()
        val joinAccompanyClazz = BuildContext.accompanyHolder[joinTargetClazz]
        val mappers = joinAccompanyClazzToMapperMap!![joinAccompanyClazz] as MutableList<(A) -> JI>
        if (CollectionUtil.isEmpty(mappers)) return emptyList()
        return mappers.toList()
    }
}