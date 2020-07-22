package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.utils.util.CollectionUtil
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * abbreviations:
 * A        accompany
 * JI       joinIndex
 * JM       joinModel
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class JoinAccessor<A: Any, JI: Any, JM: Any>(
    private val joinClazz: KClass<Any>
) : BaseJoinAccessor<A, JI, JM>(joinClazz) {

    override val jmIsTargetRelated: Boolean get() = false

    override val allCached: Map<JI, JM>
        get() = modelBuilder.getJoinCacheMap(joinClazz) as Map<JI, JM>

    override fun get(accompanies: Collection<A>): Map<A, Map<JI, JM>> {
        val mappers = getMappers<JM>(accompanies)

        val aToJis : Map<A, Set<JI>> = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val jis = aToJis.values.stream().flatMap { it.stream() }.toList()

        val cached = allCached.filterKeys { jis.contains(it) }
        val unCachedIndices = jis.filter { !cached.keys.contains(it) }

        val joinIndexToJoinModelMap = mutableMapOf<JI, JM>()
        joinIndexToJoinModelMap.putAll(cached)

        if (CollectionUtil.isNotEmpty(unCachedIndices)) {
            val builder = BuildContext.builderHolder[joinClazz]
                    as (Collection<JI>) -> Map<JI, JM>
            val buildJoinIndexToJoinModelMap = builder.invoke(unCachedIndices)
            modelBuilder.putAllJoinCacheMap(joinClazz, buildJoinIndexToJoinModelMap) // 入缓存
            joinIndexToJoinModelMap.putAll(buildJoinIndexToJoinModelMap)
        }

        return aToJis.mapValues { targetToJoinIndices -> joinIndexToJoinModelMap
            .filter { joinIndexToJoin -> targetToJoinIndices.value.contains(joinIndexToJoin.key) } }
    }

}