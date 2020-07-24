package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilder

/**
 * abbreviations:
 * A        accompany
 * AI       accompanyIndex
 * OJM      outJoinModel: support [Collection] and [Map] and [Any] and ...
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class OutJoinAccessor<A: Any, AI: Any, OJM: Any>(
    private val outJoinPoint: String
) : BaseOutJoinAccessor<A, AI, OJM>(outJoinPoint) {

    override val allCached: Map<A, OJM>
        get() = modelBuilder().getOutJoinCacheMap(outJoinPoint) as Map<A, OJM>

    override fun buildAToOjm(
        accompanies: Collection<A>,
        unCachedAis: Collection<AI>,
        aiToA: Map<AI, A>
    ): Map<A, OJM> {
        val mapper = getMapper<OJM>(accompanies)
        val buildAiToOjm = mapper.invoke(unCachedAis)
        val buildAToOjm =  buildAiToOjm.mapKeys { aiToA[it.key]!! }
        modelBuilder().putAllOutJoinCacheMap(outJoinPoint, buildAToOjm) // 入缓存
        return buildAToOjm
    }

    companion object {
        fun <A: Any, AI:Any, OJM: Any> outJoinAccessor(
            outJoinPoint: String
        ): BaseOutJoinAccessor<A, AI, OJM> =
            OutJoinAccessor(outJoinPoint)
    }
}