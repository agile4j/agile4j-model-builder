package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * @param A accompany
 * @param JI joinIndex
 * @param JT joinTarget
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class JoinTargetAccessor<A: Any, JI, JT: Any>(private val joinTargetClazz: KClass<Any>) : IAccessor<A, Map<JI, JT>> {

    override fun get(sources: Collection<A>): Map<A, Map<JI, JT>> {
        val modelBuilder = modelBuilderScopeKey.get()
            ?: throw ModelBuildException("modelBuilderScopeKey not init")
        val accompanies = sources.toSet()
        val mappers = getMappers(accompanies)

        val accompanyToJoinIndicesMap = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val joinIndices = accompanyToJoinIndicesMap.values.stream()
            .flatMap { it.stream() }.collect(Collectors.toSet())

        val allCacheMap = modelBuilder.getJoinTargetCacheMap(joinTargetClazz) as Map<JI, JT>
        val cached = allCacheMap.filterKeys { joinIndices.contains(it) }
        val unCachedJoinIndices = joinIndices.filter { !cached.keys.contains(it) }

        val joinIndexToJoinTargetMap = mutableMapOf<JI, JT>()
        joinIndexToJoinTargetMap.putAll(cached)

        if (CollectionUtil.isNotEmpty(unCachedJoinIndices)) {
            modelBuilder buildMulti joinTargetClazz by unCachedJoinIndices
            val buildJoinIndexToJoinTargetMap = modelBuilder.indexToTargetMap as Map<out JI, JT>
            modelBuilder.putAllJoinTargetCacheMap(joinTargetClazz, buildJoinIndexToJoinTargetMap)  // 入缓存
            joinIndexToJoinTargetMap.putAll(buildJoinIndexToJoinTargetMap)
        }

        return accompanyToJoinIndicesMap.mapValues { accompanyToJoinIndices -> joinIndexToJoinTargetMap
            .filter { joinIndexToJoinTarget -> accompanyToJoinIndices.value.contains(joinIndexToJoinTarget.key) } }
    }

    private fun getMappers(accompanies: Set<A>): List<(A) -> JI> {
        if (CollectionUtil.isEmpty(accompanies)) throw ModelBuildException("accompanies is empty")
        val accompanyClazz = accompanies.elementAt(0)::class
        val joinAccompanyClazzToMapperMap = BuildContext.joinHolder[accompanyClazz]
        if (MapUtil.isEmpty(joinAccompanyClazzToMapperMap)) throw ModelBuildException("joinAccompanyClazzToMapperMap is empty")
        val joinAccompanyClazz = BuildContext.accompanyHolder[joinTargetClazz]
        val mappers = joinAccompanyClazzToMapperMap!![joinAccompanyClazz] as MutableList<(A) -> JI>
        if (CollectionUtil.isEmpty(mappers)) throw ModelBuildException("mappers is empty")
        return mappers.toList()
    }
}