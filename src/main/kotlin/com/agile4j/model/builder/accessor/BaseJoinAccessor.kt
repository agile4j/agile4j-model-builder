package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import kotlin.reflect.KClass
import kotlin.streams.toList

/**
 * abbreviations:
 * A        accompany
 * JI       joinIndex
 * JM       joinModel: if [JoinAccessor] JM else [JoinTargetAccessor] JT
 * @author liurenpeng
 * Created on 2020-07-22
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseJoinAccessor<A: Any, JI:Any, JM: Any>(
    private val joinClazz: KClass<Any>
) {

    protected val modelBuilder = modelBuilderScopeKey.get() ?: err("modelBuilderScopeKey not init")

    abstract val allCached: Map<JI, JM>

    abstract val jmIsTargetRelated: Boolean

    fun get(accompanies: Collection<A>): Map<A, Map<JI, JM>> {
        val mappers = getMappers(accompanies)
        val aToJis = accompanies.map { a ->
            a to mappers.map { mapper -> (mapper.invoke(a)) }.toSet()}.toMap()
        val jis = aToJis.values.stream().flatMap { it.stream() }.toList().toSet()

        val cached = allCached.filterKeys { jis.contains(it) }
        val unCachedJis = jis.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedJis)) return parseResult(aToJis, cached)

        return parseResult(aToJis, cached + buildJiToJm(unCachedJis))
    }

    protected abstract fun buildJiToJm(unCachedJis: Collection<JI>): Map<JI, JM>

    private fun getRealJoinClazz(joinClazz: KClass<Any>) =
        if (jmIsTargetRelated) BuildContext.tToAHolder[joinClazz] else joinClazz

    private fun parseResult(aToJis: Map<A, Set<JI>>, jiToJm: Map<JI, JM>) =
        aToJis.mapValues { a2Jis -> jiToJm.filter { ji2jm -> a2Jis.value.contains(ji2jm.key) } }

    private fun getMappers(
        accompanies: Collection<A>
    ): List<(A) -> JI> {
        if (CollectionUtil.isEmpty(accompanies)) err("accompanies is empty")
        val accompanyClazz = accompanies.first()::class
        val joinClazzToMapperMap = BuildContext.joinHolder[accompanyClazz]
        if (MapUtil.isEmpty(joinClazzToMapperMap)) err("joinClazzToMapperMap is empty")
        val mappers = joinClazzToMapperMap!![getRealJoinClazz(joinClazz)]
        if (CollectionUtil.isEmpty(mappers)) err("mappers is empty")
        return mappers!!.toList() as List<(A) -> JI>
    }
}