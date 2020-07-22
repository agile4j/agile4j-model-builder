package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.utils.util.CollectionUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * abbreviations:
 * A        accompany
 * JI       joinIndex
 * JT       joinTarget
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class JoinTargetAccessor<A: Any, JI:Any, JT: Any>(
    private val joinClazz: KClass<Any>
) : BaseJoinAccessor<A, JI, JT>(joinClazz) {

    override val jmIsTargetRelated: Boolean get() = true

    override val allCached: Map<JI, JT>
        get() = modelBuilder.getJoinTargetCacheMap(joinClazz) as Map<JI, JT>

    override fun get(accompanies: Collection<A>): Map<A, Map<JI, JT>> {
        val mappers = getMappers<JT>(accompanies)

        val aToJis = accompanies.map { it to
                mappers.map { mapper -> (mapper.invoke(it)) }.toSet()}.toMap()
        val jis = aToJis.values.stream()
            .flatMap { it.stream() }.collect(Collectors.toSet())

        val cached = allCached.filterKeys { jis.contains(it) }
        val unCachedJoinIndices = jis.filter { !cached.keys.contains(it) }

        val joinIndexToJoinTargetMap = mutableMapOf<JI, JT>()
        joinIndexToJoinTargetMap.putAll(cached)

        if (CollectionUtil.isNotEmpty(unCachedJoinIndices)) {
            modelBuilder buildMulti joinClazz by unCachedJoinIndices
            val buildJoinIndexToJoinTargetMap = modelBuilder.indexToTargetMap as Map<out JI, JT>
            modelBuilder.putAllJoinTargetCacheMap(joinClazz, buildJoinIndexToJoinTargetMap)  // 入缓存
            joinIndexToJoinTargetMap.putAll(buildJoinIndexToJoinTargetMap)
        }

        return aToJis.mapValues { accompanyToJoinIndices -> joinIndexToJoinTargetMap
            .filter { joinIndexToJoinTarget -> accompanyToJoinIndices.value.contains(joinIndexToJoinTarget.key) } }
    }

}