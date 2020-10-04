package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getAClazzByT
import com.agile4j.model.builder.build.BuildContext.getAClazzByType
import com.agile4j.model.builder.build.BuildContext.getTClazzByType
import com.agile4j.model.builder.build.BuildContext.iJPDescHolder
import com.agile4j.model.builder.build.BuildContext.rDescHolder
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes
import com.agile4j.utils.util.CollectionUtil
import java.util.*
import java.util.Objects.nonNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.stream.Collectors.toList
import java.util.stream.Collectors.toSet
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
internal class InJoinDelegate<A: Any, IJP: Any, IJR: Any>(private val mapper: (A) -> IJP?) /*: JoinDelegate<IJR>*/ {

    operator fun getValue(thisT: Any, property: KProperty<*>): IJR? {
        val thisModelBuilder = thisT.buildInModelBuilder
        val thisA = thisModelBuilder.getCurrAByT(thisT)!! as A
        val aClazz = thisA::class

        val pd = iJPDescHolder.get(mapper as (Any) -> Any?) as IJPDesc<A, IJP>
        val rd = rDescHolder.get(property)!!

        val pdEqRd = pd.eq(rd)
        try {
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
        } finally {
            Scopes.setModelBuilder(null)
        }
    }

    private fun <I, A: Any> putIToACache(
        modelBuilder: ModelBuilder,
        AClazz: KClass<A>,
        buildIToA: Map<I, A>,
        unCachedIs: Collection<I>
    ) {
        modelBuilder.putGlobalIToACache(AClazz, buildIToA)
        if (buildIToA.size == unCachedIs.size) return

        val unFetchedIs = unCachedIs.filter { !buildIToA.keys.contains(it) }.toSet()
        val unFetchedIToA = unFetchedIs.associateWith { null }
        modelBuilder.putGlobalIToACache(AClazz, unFetchedIToA)
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
        val thisIjicCache = thisModelBuilder.getGlobalIToTCache(ijtClazz, thisIjic)
        if (thisIjicCache.size == thisIjic.size) {
            val thisIjtc = thisIjicCache.values.stream()
                .filter(Objects::nonNull).map { it!! }.collect(toList())
            if (rd.isSet) {
                return thisIjtc.toSet() as IJR
            }
            if (rd.isList) {
                return thisIjtc as IJR
            }
            return thisIjtc as IJR
        }

        val allA = thisModelBuilder.currAllA as Set<A>
        val ijaClazz = getAClazzByT(ijtClazz)!! as KClass<Any>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjt = thisModelBuilder.getGlobalIToTCache(ijtClazz, ijis)
        val unCachedIs = ijis.filter { !ijiToIjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ijModelBuilder)

            ijModelBuilder buildMulti ijtClazz by unCachedIs
            val buildIjiToIjt = ijModelBuilder.currIToT
            ijiToIjt += buildIjiToIjt
        }

        val thisIjtc = thisIjic.map { iji -> ijiToIjt[iji] }.stream()
            .filter(Objects::nonNull).map { it!! }.collect(toList())
        if (rd.isSet) {
            return thisIjtc.toSet() as IJR
        }
        if (rd.isList) {
            return thisIjtc as IJR
        }
        return thisIjtc as IJR
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
        val thisIjiCache = thisModelBuilder
            .getGlobalIToTCache(ijtClazz, setOf(thisIji)) as MutableMap<IJP, IJR>
        if (thisIjiCache.size == 1) {
            return thisIjiCache[thisIji]
        }

        val allA = thisModelBuilder.currAllA as Set<A>
        val ijaClazz = getAClazzByT(ijtClazz)!! as KClass<Any>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjt = thisModelBuilder
            .getGlobalIToTCache(ijtClazz, ijis) as MutableMap<IJP, IJR>
        val unCachedIs = ijis.filter { !ijiToIjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ijModelBuilder)

            ijModelBuilder buildMulti ijtClazz by unCachedIs
            val buildIjiToIjt = ijModelBuilder.currIToT as Map<IJP, IJR>
            ijiToIjt += buildIjiToIjt
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
        val cacheResp = thisModelBuilder.getGlobalIToACache(ijaClazz, thisIjic)
        val thisIjicCache = cacheResp.cached
        if (thisIjicCache.size == thisIjic.size) {
            val thisIjac = thisIjicCache.values.stream()
                .filter(Objects::nonNull).map { it!! }.collect(toList())
            if (rd.isSet) {
                return thisIjac.toSet() as IJR
            }
            if (rd.isList) {
                return thisIjac as IJR
            }
            return thisIjac as IJR
        }

        val allA = thisModelBuilder.currAllA as Set<A>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjaCacheResp = thisModelBuilder.getGlobalIToACache(ijaClazz, ijis)
        val ijiToIja = ijiToIjaCacheResp.cached
        val unCachedIs = ijiToIjaCacheResp.unCached as Collection<IJP>
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val ijaBuilder = builderHolder[ijaClazz]
                    as (Collection<Any>) -> Map<Any, Any>
            val buildIjiToIja = ijaBuilder.invoke(unCachedIs)
            putIToACache(thisModelBuilder, ijaClazz, buildIjiToIja, unCachedIs)
            ijiToIja += buildIjiToIja
        }

        val thisIjac = thisIjic.map { iji -> ijiToIja[iji] }.stream()
            .filter(Objects::nonNull).map { it!! }.collect(toList())
        if (rd.isSet) {
            return thisIjac.toSet() as IJR
        }
        if (rd.isList) {
            return thisIjac as IJR
        }
        return thisIjac as IJR
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
        val cacheResp = thisModelBuilder
            .getGlobalIToACache(ijaClazz, setOf(thisIji))
        val thisIjiCache = cacheResp.cached as MutableMap<IJP, IJR>
        if (thisIjiCache.size == 1) {
            return thisIjiCache[thisIji]
        }

        val allA = thisModelBuilder.currAllA as Set<A>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)
        val ijiToIjaCacheResp = thisModelBuilder
            .getGlobalIToACache(ijaClazz, ijis)
        val ijiToIja = ijiToIjaCacheResp.cached as MutableMap<IJP, IJR>
        val unCachedIs = ijiToIjaCacheResp.unCached as Collection<IJP>

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val ijaBuilder = builderHolder[ijaClazz]
                    as (Collection<IJP>) -> Map<IJP, IJR>
            val buildIjiToIja = ijaBuilder.invoke(unCachedIs)
            putIToACache(thisModelBuilder, ijaClazz, buildIjiToIja, unCachedIs)
            ijiToIja += buildIjiToIja
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

        val thisIjas = (mapper.invoke(thisA) as Collection<*>).stream()
            .filter(::nonNull).map { it!! }.collect(toList())
        if (CollectionUtil.isEmpty(thisIjas)) {
            if (rd.isSet) {
                return emptySet<Any>() as IJR
            }
            if (rd.isList) {
                return emptyList<Any>() as IJR
            }
            return emptyList<Any>() as IJR
        } else {
            val thisIjasCache = thisModelBuilder.getGlobalAToTCache(ijtClazz, thisIjas)
            if (thisIjasCache.size == thisIjas.size) {
                val thisIjtc = thisIjasCache.values.stream()
                    .filter(::nonNull).map { it!! }.collect(toList())
                if (rd.isSet) {
                    return thisIjtc.toSet() as IJR
                }
                if (rd.isList) {
                    return thisIjtc as IJR
                }
                return thisIjtc as IJR
            }
        }

        val allA = thisModelBuilder.currAllA as Set<A>
        val aToIjac = allA.associateWith{ mapper.invoke(it) }
        val ijas = aToIjac.values.stream()
            .flatMap { ijac -> (ijac as Collection<*>).stream() }
            .filter(::nonNull).map { it!! }.collect(toSet())
        val ijaToIjt = thisModelBuilder.getGlobalAToTCache(ijtClazz, ijas)
        val unCachedAs = ijas.filter { !ijaToIjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedAs)) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ijModelBuilder)

            ijModelBuilder buildMulti (ijtClazz) by unCachedAs
            ijaToIjt += ijModelBuilder.currAToT
        }

        val thisIjac = aToIjac[thisA] as Collection<Any>
        val thisIjtc = thisIjac.map { ija -> ijaToIjt[ija] } as List<Any>
        if (rd.isSet) {
            return thisIjtc.toSet() as IJR
        }
        if (rd.isList) {
            return thisIjtc as IJR
        }
        return thisIjtc as IJR
    }

    // A->IJA->IJT: IJP=IJA;IJR=IJT
    private fun handleAToIjaToIjt(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisA: A
    ): IJR? {
        val ijtClazz = getTClazzByType(rd.type)!!

        val thisIja = mapper.invoke(thisA) ?: return null
        val thisIjaCache = thisModelBuilder.
            getGlobalAToTCache(ijtClazz, setOf(thisIja)) as Map<IJP, IJR>
        if (thisIjaCache.size == 1) {
            return thisIjaCache[thisIja]
        }

        val allA = thisModelBuilder.currAllA as Set<A>
        val aToIja = allA.associateWith{mapper.invoke(it)}
        val ijas = aToIja.values
        val ijaToIjt = thisModelBuilder
            .getGlobalAToTCache(ijtClazz, ijas) as MutableMap<IJP, IJR>
        val unCachedAs = ijas.filter { !ijaToIjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedAs)) {
            val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ijModelBuilder)

            ijModelBuilder buildMulti (ijtClazz) by unCachedAs
            ijaToIjt += ijModelBuilder.currAToT as Map<IJP, IJR>
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
        val ijaClazzToSingleMappers =
            BuildContext.singleInJoinHolder.computeIfAbsent(aClazz) { ConcurrentHashMap() }
        val singleMappers = ijaClazzToSingleMappers.computeIfAbsent(
            ijaClazz) { CopyOnWriteArraySet() } as MutableSet<(A) -> IJI>
        if (!pd.isColl && !singleMappers.contains(mapper as (A) -> IJI)) singleMappers.add(mapper)
        val singleAToIjis = allA.associateWith {
            singleMappers.map { mapper -> (mapper.invoke(it)) } }
        val singleIjis = singleAToIjis.values.flatten()

        val ijaClazzToMultiMappers =
            BuildContext.multiInJoinHolder.computeIfAbsent(aClazz) { ConcurrentHashMap() }
        val multiMappers = ijaClazzToMultiMappers.computeIfAbsent(
            ijaClazz) { CopyOnWriteArraySet()} as MutableSet<(A) -> Collection<IJI>>
        if (pd.isColl && !multiMappers.contains(mapper as (A) -> Collection<IJI>)) multiMappers.add(mapper)
        val multiAToIjis = allA.associateWith {
            multiMappers.map { mapper -> (mapper.invoke(it)) }.flatten()  }
        val multiIjis = multiAToIjis.values.flatten()

        return (singleIjis + multiIjis).toSet()
    }

    companion object {
        fun <A: Any, IJP: Any, IJR: Any> inJoin(mapper: (A) -> IJP?) =
            InJoinDelegate<A, IJP, IJR>(mapper)
    }
}