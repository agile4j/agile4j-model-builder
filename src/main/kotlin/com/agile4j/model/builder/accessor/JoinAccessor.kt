package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * @param JI joinIndex
 * @param JM joinModel
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class JoinAccessor<A: Any, JI, JM>(private val joinClazz: KClass<*>) : IAccessor<A, Map<JI, JM>> {

    override fun get(sources: Collection<A>): Map<A, Map<JI, JM>> {
        val modelBuilder = modelBuilderScopeKey.get()
            ?: throw ModelBuildException("modelBuilderScopeKey not init")
        val accompanies = sources.toSet()
        val mappers = getMappers(accompanies)
        if (CollectionUtil.isEmpty(mappers)) return emptyMap()

        val targetToJoinIndices : Map<A, Set<JI>> = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val joinIndices = targetToJoinIndices.values.stream().flatMap { it.stream() }.toList()

        val allCacheMap = modelBuilder.getJoinCacheMap(joinClazz) as Map<JI, JM>
        val cached = allCacheMap.filterKeys { joinIndices.contains(it) }
        val unCachedIndices = joinIndices.filter { !cached.keys.contains(it) }

        val allJoinIndexToJoinMap = mutableMapOf<JI, JM>()
        allJoinIndexToJoinMap.putAll(cached)

        if (CollectionUtil.isNotEmpty(unCachedIndices)) {
            val builder = BuildContext.builderHolder[joinClazz]
                    as (Collection<JI>) -> Map<JI, JM>
            val buildJoinIndexToJoinMap = builder.invoke(unCachedIndices)
            modelBuilder.putAllJoinCacheMap(joinClazz, buildJoinIndexToJoinMap) // 入缓存
            allJoinIndexToJoinMap.putAll(buildJoinIndexToJoinMap)
        }

        return targetToJoinIndices.mapValues { currJoinIndices -> allJoinIndexToJoinMap
            .filter { joinIndexToJoin -> currJoinIndices.value.contains(joinIndexToJoin.key) } }
    }

    private fun getMappers(accompanies: Set<A>): List<(A) -> JI> {
        if (CollectionUtil.isEmpty(accompanies)) return emptyList()
        val accompanyClazz = accompanies.elementAt(0)::class
        val joinClazzToMapperMap = BuildContext.joinHolder[accompanyClazz]
        if (MapUtil.isEmpty(joinClazzToMapperMap)) return emptyList()
        val mappers = joinClazzToMapperMap!![joinClazz] as MutableList<(A) -> JI>
        if (CollectionUtil.isEmpty(mappers)) return emptyList()
        return mappers.toList()
    }
}