package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext.getAClazzByT
import com.agile4j.model.builder.build.BuildContext.getAClazzByType
import com.agile4j.model.builder.build.BuildContext.getBuilder
import com.agile4j.model.builder.build.BuildContext.getIJPDesc
import com.agile4j.model.builder.build.BuildContext.getMultiInJoinHolder
import com.agile4j.model.builder.build.BuildContext.getRDesc
import com.agile4j.model.builder.build.BuildContext.getSingleInJoinHolder
import com.agile4j.model.builder.build.BuildContext.getTClazzByType
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.exception.getExceptionHandler
import com.agile4j.model.builder.exception.inJoinDTO
import com.agile4j.model.builder.utils.empty
import com.agile4j.model.builder.utils.flatAndFilterNonNull
import com.agile4j.model.builder.utils.merge
import com.agile4j.model.builder.utils.parseColl
import com.agile4j.utils.util.CollectionUtil
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * abbreviations:
 * I        index
 * A        accompany
 * T        target
 * C        collection
 * IJP      inJoinProvide
 * IJR      inJoinRequire
 *
 * IJ的mapper:(A) -> IJP，被认为是不需要成本的，值从model自身获取，因此结果不会进行缓存
 * 如果是需要成本，请使用EJ[ExJoinDelegate]，而非IJ[InJoinDelegate]
 *
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class InJoinDelegate<A: Any, IJP: Any, IJR: Any>(
    private val mapper: (A) -> IJP?,
    private val pruner: () -> Boolean) {

    operator fun getValue(thisT: Any, property: KProperty<*>): IJR? {
        val pd = getIJPDesc(mapper)
        val rd = getRDesc(property)
        if (!pruner()) return empty(rd) // 剪枝
        val pdEqRd = pd.eq(rd)

        val thisModelBuilder = thisT.buildInModelBuilder
        val thisA = thisModelBuilder.getCurrAByT(thisT)!! as A
        val thisI = thisModelBuilder.getCurrIByA(thisA)!!

        return try {
            handleInJoin(pd, rd, pdEqRd, thisA, thisModelBuilder, thisA::class, thisT, property)
        } catch (t: Throwable) {
            getExceptionHandler(thisT::class, thisA::class)?.handleInJoinException(
                inJoinDTO(t, thisT, thisA, thisI, property, mapper, pruner, pd, rd)) ?: throw t
        }
    }

    private fun handleInJoin(
        pd: IJPDesc<A, IJP>,
        rd: RDesc,
        pdEqRd: Boolean,
        thisA: A,
        thisModelBuilder: ModelBuilder,
        aClazz: KClass<out A>,
        thisT: Any,
        property: KProperty<*>
    ): IJR? {
        // A->IJM
        if (!pd.isColl && !rd.isColl && pdEqRd) {
            return handleAToIjm(thisA)
        }
        // A->C[IJM]
        if (pd.isColl && rd.isColl && pdEqRd) {
            return handleAToIjmc(thisA)
        }
        // A->IJA->IJT: IJP=IJA;IJR=IJT
        if (!pd.isColl && !rd.isColl && pd.isA && rd.isT) {
            return handleAToIjaToIjt(rd, thisModelBuilder, thisA)
        }
        // A->C[IJA]->C[IJT]: IJP=C[IJA];IPR=C[IJT]
        if (pd.isColl && rd.isColl && pd.isA && rd.isT) {
            return handleAToIjacToIjtc(rd, thisModelBuilder, thisA)
        }
        // A->IJI->IJA: IJP=IJI;IJR=IJA
        if (!pd.isColl && !rd.isColl && pd.isI && rd.isA) {
            return handleAToIjiToIja(rd, pd, thisModelBuilder, thisA, aClazz)
        }
        // A->C[IJI]->C[IJA]: IJP=C[IJI];IJR=C[IJA]
        if (pd.isColl && rd.isColl && pd.isI && rd.isA) {
            return handleAToIjicToIjac(rd, pd, thisModelBuilder, thisA, aClazz)
        }
        // A->IJI->IJA->IJT: IJP=IJI;IJR=IJT
        if (!pd.isColl && !rd.isColl && pd.isI && rd.isT) {
            return handleAToIjiToIjaToIjt(rd, pd, thisModelBuilder, thisA, aClazz)
        }
        // A->C[IJI]->C[IJA]->C[IJT]: IJP=C[IJI];IJR=C[IJT]
        if (pd.isColl && rd.isColl && pd.isI && rd.isT) {
            return handleAToIjicToIjacToIjtc(rd, pd, thisModelBuilder, thisA, aClazz)
        }
        err("cannot handle. mapper:$mapper. thisT:$thisT. property:$property")
    }

    // A->C[IJI]->C[IJA]->C[IJT]: IJP=C[IJI];IJR=C[IJT]
    private fun handleAToIjicToIjacToIjtc(
        rd: RDesc,
        pd: IJPDesc<A, IJP>,
        thisModelBuilder: ModelBuilder,
        thisA: A,
        aClazz: KClass<out A>
    ): IJR {
        val ijtClazz = getTClazzByType(rd.cType!!)!!

        val thisIjic = mapper.invoke(thisA) as Collection<Any>
        val thisIjicToIjtcCacheResp = thisModelBuilder.getGlobalIToTCache(ijtClazz, thisIjic)
        val thisIjicToIjtcCached = thisIjicToIjtcCacheResp.cached
        if (thisIjicToIjtcCached.size == thisIjic.size) {
            val thisIjtc = thisIjicToIjtcCached.values.filter { it != null } as List<Any>
            return parseColl(thisIjtc, rd) as IJR
        }

        val allA = thisModelBuilder.getCurrAllA() as Set<A>
        val ijaClazz = getAClazzByT(ijtClazz)!! as KClass<Any>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjtCacheResp = thisModelBuilder.getGlobalIToTCache(ijtClazz, ijis)
        var ijiToIjt: Map<Any?, Any?> = ijiToIjtCacheResp.cached
        val unCachedIs = ijiToIjtCacheResp.unCached as Collection<IJP>

        if (unCachedIs.isNotEmpty()) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            ijModelBuilder buildMulti ijtClazz by unCachedIs
            val buildIjiToIjt = ijModelBuilder.getCurrIToT()
            ijiToIjt = merge(ijiToIjt as MutableMap<Any?, Any?>, buildIjiToIjt as Map<Any?, Any?>)
        }

        val thisIjtc = thisIjic.map { iji -> ijiToIjt[iji] }.filter { it != null } as List<Any>
        return parseColl(thisIjtc, rd) as IJR
    }

    // A->IJI->IJA->IJT: IJP=IJI;IJR=IJT
    private fun handleAToIjiToIjaToIjt(
        rd: RDesc,
        pd: IJPDesc<A, IJP>,
        thisModelBuilder: ModelBuilder,
        thisA: A,
        aClazz: KClass<out A>
    ): IJR? {
        val ijtClazz = getTClazzByType(rd.type)!!

        val thisIji = mapper.invoke(thisA)
        val thisIjiToIjtCacheResp = thisModelBuilder.getGlobalIToTCache(ijtClazz, setOf(thisIji))
        val thisIjiToIjtCached = thisIjiToIjtCacheResp.cached as MutableMap<IJP, IJR>
        if (thisIjiToIjtCached.size == 1) return thisIjiToIjtCached[thisIji]

        val allA = thisModelBuilder.getCurrAllA() as Set<A>
        val ijaClazz = getAClazzByT(ijtClazz)!! as KClass<Any>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjtCacheResp = thisModelBuilder.getGlobalIToTCache(ijtClazz, ijis)
        var ijiToIjt = ijiToIjtCacheResp.cached as Map<IJP, IJR>
        val unCachedIs = ijiToIjtCacheResp.unCached as Collection<IJP>

        if (unCachedIs.isNotEmpty()) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            ijModelBuilder buildMulti ijtClazz by unCachedIs
            val buildIjiToIjt = ijModelBuilder.getCurrIToT() as Map<IJP, IJR>
            ijiToIjt = merge(ijiToIjt as MutableMap<IJP, IJR>, buildIjiToIjt)
        }

        return ijiToIjt[thisIji]
    }

    // A->C[IJI]->C[IJA]: IJP=C[IJI];IJR=C[IJA]
    private fun handleAToIjicToIjac(
        rd: RDesc,
        pd: IJPDesc<A, IJP>,
        thisModelBuilder: ModelBuilder,
        thisA: A,
        aClazz: KClass<out A>
    ): IJR {
        val ijaClazz = getAClazzByType(rd.cType!!)!!

        val thisIjic = mapper.invoke(thisA) as Collection<Any>
        val thisIjicToIjacCacheResp = thisModelBuilder.getGlobalIToACache(ijaClazz, thisIjic)
        val thisIjicToIjacCached = thisIjicToIjacCacheResp.cached
        if (thisIjicToIjacCached.size == thisIjic.size) {
            val thisIjac = thisIjicToIjacCached.values.filter { it != null } as List<Any>
            return parseColl(thisIjac, rd) as IJR
        }

        val allA = thisModelBuilder.getCurrAllA() as Set<A>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjaCacheResp = thisModelBuilder.getGlobalIToACache(ijaClazz, ijis)
        var ijiToIja: Map<Any?, Any?> = ijiToIjaCacheResp.cached
        val unCachedIs = ijiToIjaCacheResp.unCached as Collection<IJP>

        if (unCachedIs.isNotEmpty()) {
            val ijaBuilder = getBuilder<Any, Any>(ijaClazz)
            val buildIjiToIja = ijaBuilder.invoke(unCachedIs)
            putIToACache(thisModelBuilder, ijaClazz, buildIjiToIja, unCachedIs)
            ijiToIja = merge(ijiToIja as MutableMap<Any?, Any?>, buildIjiToIja as Map<Any?, Any?>)
        }

        val thisIjac = thisIjic.map { iji -> ijiToIja[iji] }.filter { it != null } as List<Any>
        return parseColl(thisIjac, rd) as IJR
    }

    // A->IJI->IJA: IJP=IJI;IJR=IJA
    private fun handleAToIjiToIja(
        rd: RDesc,
        pd: IJPDesc<A, IJP>,
        thisModelBuilder: ModelBuilder,
        thisA: A,
        aClazz: KClass<out A>
    ): IJR? {
        val ijaClazz = getAClazzByType(rd.type)!!
        val thisIji = mapper.invoke(thisA)
        val thisIjiToIjaCacheResp = thisModelBuilder.getGlobalIToACache(ijaClazz, setOf(thisIji))
        val thisIjiToIjaCached = thisIjiToIjaCacheResp.cached as MutableMap<IJP, IJR>
        if (thisIjiToIjaCached.size == 1) return thisIjiToIjaCached[thisIji]

        val allA = thisModelBuilder.getCurrAllA() as Set<A>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjaCacheResp = thisModelBuilder.getGlobalIToACache(ijaClazz, ijis)
        var ijiToIja = ijiToIjaCacheResp.cached as Map<IJP, IJR>
        val unCachedIs = ijiToIjaCacheResp.unCached as Collection<IJP>

        if (unCachedIs.isNotEmpty()) {
            val ijaBuilder = getBuilder<IJR, IJP>(ijaClazz)
            val buildIjiToIja = ijaBuilder.invoke(unCachedIs)
            putIToACache(thisModelBuilder, ijaClazz, buildIjiToIja, unCachedIs)
            ijiToIja = merge(ijiToIja as MutableMap<IJP, IJR>, buildIjiToIja)
        }

        return ijiToIja[thisIji]
    }

    // A->C[IJA]->C[IJT]: IJP=C[IJA];IPR=C[IJT]
    private fun handleAToIjacToIjtc(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisA: A
    ): IJR {
        val ijtClazz = getTClazzByType(rd.cType!!)!!

        val thisIjac = (mapper.invoke(thisA) as Collection<*>).filter { it != null } as List<Any>
        if (CollectionUtil.isEmpty(thisIjac)) {
            return parseColl(emptyList<Any>(), rd) as IJR
        } else {
            val thisIjacToIjtcCacheResp = thisModelBuilder.getGlobalAToTCache(ijtClazz, thisIjac)
            val thisIjacToIjtcCached = thisIjacToIjtcCacheResp.cached
            if (thisIjacToIjtcCached.size == thisIjac.size) {
                val thisIjtc = thisIjacToIjtcCached.values.filter { it != null } as List<Any>
                return parseColl(thisIjtc, rd) as IJR
            }
        }

        val allA = thisModelBuilder.getCurrAllA() as Set<A>
        val aToIjac = allA.associateWith{ mapper.invoke(it) }
        val ijac = flatAndFilterNonNull(aToIjac.values)
        val ijaToIjtCacheResp = thisModelBuilder.getGlobalAToTCache(ijtClazz, ijac)
        var ijaToIjt: Map<Any?, Any?> = ijaToIjtCacheResp.cached
        val unCachedAs = ijaToIjtCacheResp.unCached as Collection<Any>

        if (unCachedAs.isNotEmpty()) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            ijModelBuilder buildMulti (ijtClazz) by unCachedAs
            ijaToIjt = merge(ijaToIjt as MutableMap<Any?, Any?>, ijModelBuilder.getCurrAToT() as Map<Any?, Any?>)
        }

        val thisIjtc = thisIjac.map { ija -> ijaToIjt[ija] } as List<Any>
        return parseColl(thisIjtc, rd) as IJR
    }

    // A->IJA->IJT: IJP=IJA;IJR=IJT
    private fun handleAToIjaToIjt(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisA: A
    ): IJR? {
        val ijtClazz = getTClazzByType(rd.type)!!

        val thisIja = mapper.invoke(thisA) ?: return null
        val cacheResp = thisModelBuilder.getGlobalAToTCache(ijtClazz, setOf(thisIja))
        val thisIjaToIjtCached = cacheResp.cached as Map<IJP, IJR>
        if (thisIjaToIjtCached.size == 1) return thisIjaToIjtCached[thisIja]

        val allA = thisModelBuilder.getCurrAllA() as Set<A>
        val aToIja = allA.associateWith{mapper.invoke(it)}
        val ijas = aToIja.values
        val ijaToIjtCacheResp = thisModelBuilder.getGlobalAToTCache(ijtClazz, ijas)
        var ijaToIjt = ijaToIjtCacheResp.cached as Map<IJP, IJR>
        val unCachedAs = ijaToIjtCacheResp.unCached as Collection<IJP?>

        if (unCachedAs.isNotEmpty()) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            ijModelBuilder buildMulti (ijtClazz) by unCachedAs
            ijaToIjt = merge(ijaToIjt as MutableMap<IJP, IJR>, ijModelBuilder.getCurrAToT() as Map<IJP, IJR>)
        }
        return ijaToIjt[aToIja[thisA]]
    }

    // A->IJM
    private fun handleAToIjm(thisA: A): IJR? {
        return mapper.invoke(thisA) as IJR?
    }

    // A->C[IJM]
    private fun handleAToIjmc(thisA: A): IJR? {
        return mapper.invoke(thisA) as IJR?
    }

    private fun <IJI> extractIjis(
        aClazz: KClass<out A>,
        ijaClazz: KClass<Any>,
        allA: Collection<A>,
        pd: IJPDesc<A, IJP>
    ): Collection<IJI> {
        val ijaClazzToSingleMappers = getSingleInJoinHolder(aClazz)
        val singleMappers = ijaClazzToSingleMappers.computeIfAbsent(
            ijaClazz) { CopyOnWriteArraySet() } as MutableSet<(A) -> IJI>
        if (!pd.isColl) singleMappers.add(mapper as (A) -> IJI)
        val singleAToIjis = allA.associateWith {
            singleMappers.map { mapper -> (mapper.invoke(it)) } }
        val singleIjis = singleAToIjis.values.flatten()

        val ijaClazzToMultiMappers = getMultiInJoinHolder(aClazz)
        val multiMappers = ijaClazzToMultiMappers.computeIfAbsent(
            ijaClazz) { CopyOnWriteArraySet()} as MutableSet<(A) -> Collection<IJI>>
        if (pd.isColl) multiMappers.add(mapper as (A) -> Collection<IJI>)
        val multiAToIjis = allA.associateWith {
            multiMappers.map { mapper -> (mapper.invoke(it)) }.flatten()  }
        val multiIjis = multiAToIjis.values.flatten()

        val result = mutableSetOf<IJI>()
        result.addAll(singleIjis)
        result.addAll(multiIjis)
        return result
    }

    private fun <I, A: Any> putIToACache(
        modelBuilder: ModelBuilder,
        AClazz: KClass<A>,
        buildIToA: Map<I, A>,
        unCachedIs: Collection<I>
    ) {
        modelBuilder.putGlobalIToACache(AClazz, buildIToA)
        if (buildIToA.size == unCachedIs.size) return

        val unFetchedIs = unCachedIs.filter { !buildIToA.keys.contains(it) }
        val unFetchedIToA = unFetchedIs.associateWith { null }
        modelBuilder.putGlobalIToACache(AClazz, unFetchedIToA)
    }

    companion object {
        fun <A: Any, IJP: Any, IJR: Any> inJoin(
            mapper: (A) -> IJP?
        ): InJoinDelegate<A, IJP, IJR> = inJoin(mapper){ true }

        /**
         * @param pruner 剪枝器
         * 值为false直接返回空:null or empty collection
         * 值为true才真实求值
         */
        fun <A: Any, IJP: Any, IJR: Any> inJoin(
            mapper: (A) -> IJP?,
            pruner: () -> Boolean
        ): InJoinDelegate<A, IJP, IJR> = InJoinDelegate(mapper, pruner)
    }
}