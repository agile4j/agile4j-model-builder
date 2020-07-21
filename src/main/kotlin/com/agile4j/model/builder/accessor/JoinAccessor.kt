package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * @param A accompany
 * @param JI joinIndex
 * @param JM joinModel
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class JoinAccessor<A: Any, JI, JM>(
    private val joinClazz: KClass<Any>
) : IAccessor<A, Map<JI, JM>> {

    private val modelBuilder = ITargetDelegate.ScopeKeys.modelBuilderScopeKey.get()
        ?: throw ModelBuildException("modelBuilderScopeKey not init")

    override fun get(accompanies: Collection<A>): Map<A, Map<JI, JM>> {
        val mappers = getMappers(accompanies)

        val targetToJoinIndicesMap : Map<A, Set<JI>> = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val joinIndices = targetToJoinIndicesMap.values.stream().flatMap { it.stream() }.toList()

        val allCacheMap = modelBuilder.getJoinCacheMap(joinClazz) as Map<JI, JM>
        val cached = allCacheMap.filterKeys { joinIndices.contains(it) }
        val unCachedIndices = joinIndices.filter { !cached.keys.contains(it) }

        val joinIndexToJoinModelMap = mutableMapOf<JI, JM>()
        joinIndexToJoinModelMap.putAll(cached)

        if (CollectionUtil.isNotEmpty(unCachedIndices)) {
            val builder = BuildContext.builderHolder[joinClazz]
                    as (Collection<JI>) -> Map<JI, JM>
            val buildJoinIndexToJoinModelMap = builder.invoke(unCachedIndices)
            modelBuilder.putAllJoinCacheMap(joinClazz, buildJoinIndexToJoinModelMap) // 入缓存
            joinIndexToJoinModelMap.putAll(buildJoinIndexToJoinModelMap)
        }

        return targetToJoinIndicesMap.mapValues { targetToJoinIndices -> joinIndexToJoinModelMap
            .filter { joinIndexToJoin -> targetToJoinIndices.value.contains(joinIndexToJoin.key) } }
    }

    private fun getMappers(accompanies: Collection<A>): List<(A) -> JI> {
        if (CollectionUtil.isEmpty(accompanies)) throw ModelBuildException("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val joinClazzToMapperMap = BuildContext.joinHolder[accompanyClazz]
        if (MapUtil.isEmpty(joinClazzToMapperMap)) throw ModelBuildException("joinClazzToMapperMap is empty")
        val mappers = joinClazzToMapperMap!![joinClazz] as MutableList<(A) -> JI>
        if (CollectionUtil.isEmpty(mappers)) throw ModelBuildException("mappers is empty")
        return mappers.toList()
    }
}