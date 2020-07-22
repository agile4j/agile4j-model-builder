package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
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

    override fun buildJiToJm(unCachedJis: Collection<JI>): Map<JI, JT> {
        modelBuilder buildMulti joinClazz by unCachedJis
        val buildJiToJt = modelBuilder.indexToTargetMap as Map<JI, JT>
        modelBuilder.putAllJoinTargetCacheMap(joinClazz, buildJiToJt)  // 入缓存
        return buildJiToJt
    }

    /*override fun get(accompanies: Collection<A>): Map<A, Map<JI, JT>> {
        val mappers = getMappers(accompanies)
        val aToJis = accompanies.map { a ->
            a to mappers.map { mapper -> (mapper.invoke(a)) }.toSet()}.toMap()
        val jis = aToJis.values.stream().flatMap { it.stream() }.toList().toSet()

        val cached = allCached.filterKeys { jis.contains(it) }
        val unCachedJis = jis.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedJis)) return parseResult(aToJis, cached)

        modelBuilder buildMulti joinClazz by unCachedJis
        val buildJiToJt = modelBuilder.indexToTargetMap as Map<out JI, JT>
        modelBuilder.putAllJoinTargetCacheMap(joinClazz, buildJiToJt)  // 入缓存

        return parseResult(aToJis, cached + buildJiToJt)
    }*/

}