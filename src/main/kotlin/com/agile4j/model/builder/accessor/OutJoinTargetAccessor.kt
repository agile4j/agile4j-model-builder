package com.agile4j.model.builder.accessor

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.utils.firstValue
import com.agile4j.model.builder.utils.reverseKV
import com.agile4j.utils.util.MapUtil
import java.util.stream.Collectors
import kotlin.reflect.KClass

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
@Suppress("UNCHECKED_CAST")
internal class OutJoinTargetAccessor<A : Any, AI : Any, OJA : Any, OJT : Any, OJARM : Any, OJTRM : Any>(
    private val outJoinPoint: String
) : BaseOutJoinAccessor<A, AI, OJTRM>(outJoinPoint) {

    override val allCached: Map<A, OJTRM>
        get() = modelBuilder.getOutJoinTargetCacheMap(outJoinPoint) as Map<A, OJTRM>

    override fun buildAToOjm(
        accompanies: Collection<A>,
        unCachedAis: Collection<AI>,
        aiToA: Map<AI, A>
    ): Map<A, OJTRM> {
        val mapper = getMapper<OJARM>(accompanies)
        val buildAiToOjarm = mapper.invoke(unCachedAis) // call biz sys
        val buildAToOjarm = buildAiToOjarm.mapKeys { aiToA[it.key]!! }
        if (MapUtil.isEmpty(buildAToOjarm)) return emptyMap()

        val ojarm = buildAToOjarm.firstValue()!!
        val isCollection = ojarm is Collection<*>
        val ojaClazz = getOjaClazz(ojarm)
        val ojtClazz = BuildContext.tToAHolder.reverseKV()[ojaClazz] as KClass<OJT>

        val ojas = getOjas(isCollection, buildAToOjarm)
        modelBuilder buildMulti ojtClazz by ojas
        val ojaToOjt = modelBuilder.accompanyToTargetMap
        val buildAToOjm = getAToOjtrmMap(isCollection, buildAToOjarm, ojaToOjt)
        modelBuilder.putAllOutJoinTargetCacheMap(outJoinPoint, buildAToOjm) // 入缓存
        return buildAToOjm
    }

    private fun getAToOjtrmMap(
        isCollection: Boolean,
        aToOjarmMap: Map<A, OJARM>,
        ojaToOjtMap: Map<Any, Any>
    ): Map<A, OJTRM> =
        if (!isCollection) {
            aToOjarmMap.mapValues { v -> ojaToOjtMap[v] }
        } else {
            aToOjarmMap.mapValues { e ->
                val collValue = e.value as Collection<Any>
                collValue.map { v -> ojaToOjtMap[v] }
            }
        } as Map<A, OJTRM>

    private fun getOjas(
        isCollection: Boolean,
        buildAToOJARMMap: Map<A, OJARM>
    ): Collection<OJA> =
        if (!isCollection) {
            buildAToOJARMMap.values as Collection<OJA>
        } else {
            buildAToOJARMMap.values.stream().flatMap {
                (it as Collection<OJA>).stream()
            }.collect(Collectors.toList()) as Collection<OJA>
        }

    private fun getOjaClazz(
        ojarm: OJARM
    ): KClass<OJA> =
        if (ojarm !is Collection<*>) {
            ojarm::class
        } else {
            (ojarm as Collection<Any>).first()::class
        } as KClass<OJA>

    companion object {
        fun outJoinTargetAccessor(outJoinPoint: String): BaseOutJoinAccessor<Any, Any, Any> =
            OutJoinTargetAccessor<Any, Any, Any, Any, Any, Any>(outJoinPoint)
    }
}