package com.agile4j.model.builder.accessor

/**
 * abbreviations:
 * A        accompany
 * AI       accompanyIndex
 * OJA      outJoinAccompany
 * OJT      outJoinTarget
 * OJARM    outJoinAccompanyRelatedModel: OJA or Collection<OJA>
 * OJTRM    outJoinTargetRelatedModel: OJT or Collection<OJT>
 * @author liurenpeng
 * Created on 2020-06-18
 */
/*
@Suppress("UNCHECKED_CAST")
internal class ExJoinTargetAccessor<A: Any, I: Any, OJA: Any, OJT: Any, EJP: Any, EJR: Any>(
    private val mapper: (I) -> EJR
) : BaseExJoinAccessor<A, I, EJR>(mapper) {

    override val jmIsTargetRelated: Boolean get() = true

    override val allCached: Map<A, EJR>
        get() = modelBuilder().getOutJoinTargetCacheMap(outJoinPoint) as Map<A, EJR>

    override fun buildAToOjm(
        accompanies: Collection<A>,
        unCachedAis: Collection<I>,
        aiToA: Map<I, A>
    ): Map<A, EJR> {
        val mapper = getMapper<EJP>(accompanies)
        val buildAiToOjarm = mapper.invoke(unCachedAis) // call biz sys
        val buildAToOjarm = buildAiToOjarm.mapKeys { aiToA[it.key]!! }
        if (MapUtil.isEmpty(buildAToOjarm)) return emptyMap()

        val ojarm = buildAToOjarm.firstValue()!!
        val isCollection = ojarm is Collection<*>
        val ojaClazz = getOjaClazz(ojarm)

        // TODO this err a=>t是1对多的
        val ojtClazz = BuildContext.tToAHolder.reverseKV()[ojaClazz] as KClass<OJT>

        val ojas = getOjas(isCollection, buildAToOjarm)
        modelBuilder() buildMulti ojtClazz by ojas
        val ojaToOjt = modelBuilder().aToT
        val buildAToOjm = getAToOjtrmMap(isCollection, buildAToOjarm, ojaToOjt)
        modelBuilder().putAllOutJoinTargetCacheMap(outJoinPoint, buildAToOjm) // 入缓存
        return buildAToOjm
    }

    private fun getAToOjtrmMap(
        isCollection: Boolean,
        aToOjarmMap: Map<A, EJP>,
        ojaToOjtMap: Map<Any, Any>
    ): Map<A, EJR> =
        if (!isCollection) {
            aToOjarmMap.mapValues { v -> ojaToOjtMap[v] }
        } else {
            aToOjarmMap.mapValues { e ->
                val collValue = e.value as Collection<Any>
                collValue.map { v -> ojaToOjtMap[v] }
            }
        } as Map<A, EJR>

    private fun getOjas(
        isCollection: Boolean,
        buildAToOJARMMap: Map<A, EJP>
    ): Collection<OJA> =
        if (!isCollection) {
            buildAToOJARMMap.values as Collection<OJA>
        } else {
            buildAToOJARMMap.values.stream().flatMap {
                (it as Collection<OJA>).stream()
            }.collect(Collectors.toList()) as Collection<OJA>
        }

    private fun getOjaClazz(
        ojarm: EJP
    ): KClass<OJA> =
        if (ojarm !is Collection<*>) {
            ojarm::class
        } else {
            (ojarm as Collection<Any>).first()::class
        } as KClass<OJA>

    companion object {
        fun <A: Any, I: Any, EJP: Any, EJR: Any> outJoinTargetAccessor(
            mapper: (I) -> EJP
        ): BaseExJoinAccessor<A, I, EJR> = ExJoinTargetAccessor<A, I, Any, Any, Any, EJR>(mapper)
    }
}*/
