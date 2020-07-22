package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.utils.reverseKV
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil

/**
 * abbreviations:
 * A        accompany
 * AI       accompanyIndex
 * OJM      outJoinModel: if [OutJoinAccessor] OJM else [OutJoinTargetAccessor] OJTRM
 * @author liurenpeng
 * Created on 2020-07-21
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseOutJoinAccessor<A: Any, AI:Any, OJM: Any>(
    private val outJoinPoint: String) {

    protected val modelBuilder = modelBuilderScopeKey.get() ?: err("modelBuilderScopeKey not init")

    abstract val allCached: Map<A, OJM>

    fun get(accompanies: Collection<A>): Map<A, OJM> {
        val cached = allCached.filterKeys { accompanies.contains(it) }
        val unCachedAs = accompanies.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedAs)) return cached // all cached

        val accompanyClazz = accompanies.first()::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> AI
        val aToAi : Map<A, AI> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val aiToA = aToAi.reverseKV()
        val unCachedAis = unCachedAs.map { aToAi[it]!!}.toSet()
        if (CollectionUtil.isEmpty(unCachedAis)) return cached // all cached

        return cached + buildAToOjm(accompanies, unCachedAis, aiToA)
    }

    protected abstract fun buildAToOjm(
        accompanies: Collection<A>, unCachedAis: Collection<AI>, aiToA: Map<AI, A>): Map<A, OJM>

    /**
     * @param OJX: if [OutJoinAccessor] OJM else if [OutJoinTargetAccessor] OJARM
     */
    protected fun <OJX> getMapper(
        accompanies: Collection<A>
    ): (Collection<AI>) -> Map<AI, OJX> {
        if (CollectionUtil.isEmpty(accompanies)) err("accompanies is empty")
        val accompanyClazz = accompanies.first()::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as MutableMap<String, (Collection<AI>) -> Map<AI, OJX>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) err("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinPoint]
            ?: err("not found matched mapper. outJoinPoint:$outJoinPoint")
    }
}