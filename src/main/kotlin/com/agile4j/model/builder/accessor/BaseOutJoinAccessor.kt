package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.utils.reverseKV
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil

/**
 * @author liurenpeng
 * Created on 2020-07-21
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseOutJoinAccessor<A: Any, AI:Any, OJM: Any>(
    private val outJoinPoint: String) {

    protected val modelBuilder = modelBuilderScopeKey.get()
        ?: throw ModelBuildException("modelBuilderScopeKey not init")

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

        val buildAToOjm = buildAToOjm(accompanies, unCachedAis, aiToA)
        modelBuilder.putAllOutJoinCacheMap(outJoinPoint, buildAToOjm) // 入缓存
        return cached + buildAToOjm
    }

    protected abstract fun buildAToOjm(
        accompanies: Collection<A>, unCachedAis: Collection<AI>, aiToA: Map<AI, A>): Map<A, OJM>

    /**
     * @param OJ = if [OutJoinAccessor] OJM else [OutJoinTargetAccessor] OJARM
     */
    protected fun <OJ> getMapper(
        accompanies: Collection<A>
    ): (Collection<AI>) -> Map<AI, OJ> {
        if (CollectionUtil.isEmpty(accompanies)) ModelBuildException.err("accompanies is empty")
        val accompanyClazz = accompanies.first()::class
        val outJoinPointToMapperMap = BuildContext
            .outJoinHolder[accompanyClazz] as MutableMap<String, (Collection<AI>) -> Map<AI, OJ>>
        if (MapUtil.isEmpty(outJoinPointToMapperMap)) ModelBuildException.err("outJoinPointToMapperMap is empty")
        return outJoinPointToMapperMap[outJoinPoint]
            ?: ModelBuildException.err("not found matched mapper. outJoinPoint:$outJoinPoint")
    }
}