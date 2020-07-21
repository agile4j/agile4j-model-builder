package com.agile4j.model.builder.accessor

/**
 * abbreviations:
 * A        accompany
 * AI       accompanyIndex
 * OJM      outJoinModel: support [Collection]
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class OutJoinAccessor<A: Any, AI: Any, OJM: Any>(
    private val outJoinPoint: String
) : BaseAccessor<A, AI, OJM>(outJoinPoint) {

    override val allCached: Map<A, OJM>
        get() = modelBuilder.getOutJoinCacheMap(outJoinPoint) as Map<A, OJM>

    override fun buildAToOjm(
        accompanies: Collection<A>,
        unCachedAis: Collection<AI>,
        aiToA: Map<AI, A>
    ): Map<A, OJM> {
        val mapper = getMapper<OJM>(accompanies)
        val buildAiToOjm = mapper.invoke(unCachedAis)
        return buildAiToOjm.mapKeys { aiToA[it.key]!! }
    }
}