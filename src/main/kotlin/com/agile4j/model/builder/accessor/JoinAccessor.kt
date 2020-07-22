package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext.builderHolder
import kotlin.reflect.KClass

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

    override fun buildJiToJm(unCachedJis: Collection<JI>): Map<JI, JM> {
        val builder = builderHolder[joinClazz] as (Collection<JI>) -> Map<JI, JM>
        val buildJiToJm = builder.invoke(unCachedJis)
        modelBuilder.putAllJoinCacheMap(joinClazz, buildJiToJm) // 入缓存
        return buildJiToJm
    }

    /*override fun get(accompanies: Collection<A>): Map<A, Map<JI, JM>> {
        val mappers = getMappers(accompanies)
        val aToJis = accompanies.map { a ->
            a to mappers.map { mapper -> (mapper.invoke(a)) }.toSet()}.toMap()
        val jis = aToJis.values.stream().flatMap { it.stream() }.toList().toSet()

        val cached = allCached.filterKeys { jis.contains(it) }
        val unCachedJis = jis.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedJis)) return parseResult(aToJis, cached)

        val builder = BuildContext.builderHolder[joinClazz]
                as (Collection<JI>) -> Map<JI, JM>
        val buildJiToJm = builder.invoke(unCachedJis)
        modelBuilder.putAllJoinCacheMap(joinClazz, buildJiToJm) // 入缓存


        return parseResult(aToJis, cached + buildJiToJm)
    }*/

}