package com.agile4j.model.builder.accessor

/**
 * abbreviations:
 * A        accompany
 * AI       accompanyIndex
 * OJM      outJoinModel: if [ExJoinAccessor] OJM else [ExJoinTargetAccessor] OJTRM
 * @author liurenpeng
 * Created on 2020-07-21
 */
/*
@Suppress("UNCHECKED_CAST")
abstract class BaseExJoinAccessor<A: Any, I:Any, EJR: Any>(
    private val ejClazz: KClass<Any>
) {

    abstract val allCached: Map<A, EJR>

    abstract val jmIsTargetRelated: Boolean

    fun get(accompanies: Collection<A>): Map<A, EJR> {
        val cached = allCached.filterKeys { accompanies.contains(it) }
        val unCachedAs = accompanies.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedAs)) return cached // all cached

        val accompanyClazz = accompanies.first()::class
        val indexer = BuildContext.indexerHolder[accompanyClazz] as (A) -> I
        val aToAi : Map<A, I> = accompanies.map { it to indexer.invoke(it) }.toMap()
        val aiToA = aToAi.reverseKV()
        val unCachedAis = unCachedAs.map { aToAi[it]!!}.toSet()
        if (CollectionUtil.isEmpty(unCachedAis)) return cached // all cached

        return cached + buildAToOjm(accompanies, unCachedAis, aiToA)
    }

    protected abstract fun buildAToOjm(
        accompanies: Collection<A>, unCachedAis: Collection<I>, aiToA: Map<I, A>): Map<A, EJR>

    private fun getRealJoinClazz(joinClazz: KClass<Any>) =
        if (jmIsTargetRelated) BuildContext.tToAHolder[joinClazz] else joinClazz

    */
/**
     * @param EJ: if [ExJoinAccessor] OJM else if [ExJoinTargetAccessor] OJARM
     *//*

    protected fun <EJ> getMapper(
        accompanies: Collection<A>
    ): List<(Collection<I>) -> Map<I, EJ>> {
        if (CollectionUtil.isEmpty(accompanies)) err("accompanies is empty")
        val aClazz = accompanies.first()::class
        val ojClazzToMapperMap = BuildContext.exJoinHolder[aClazz]
        if (MapUtil.isEmpty(ojClazzToMapperMap)) err("ojClazzToMapperMap is empty")
        val mappers = ojClazzToMapperMap!![getRealJoinClazz(ejClazz)]
        if (CollectionUtil.isEmpty(mappers)) err("mappers is empty")
        return mappers!!.toList() as List<(Collection<I>) -> Map<I, EJ>>
        //  as MutableMap<String, (Collection<AI>) -> Map<AI, EJ>>
        */
/*if (MapUtil.isEmpty(ojClazzToMapperMap)) err("outJoinPointToMapperMap is empty")
        return ojClazzToMapperMap[outJoinPoint]
            ?: err("not found matched mapper. outJoinPoint:$outJoinPoint")*//*

    }
}*/
