package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getA
import com.agile4j.model.builder.build.BuildContext.getT
import com.agile4j.model.builder.build.BuildContext.tToAHolder
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes
import com.agile4j.utils.util.CollectionUtil
import java.util.Objects.nonNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
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
class InJoinDelegate<A: Any, IJP: Any, IJR: Any>(private val mapper: (A) -> IJP) /*: JoinDelegate<IJR>*/ {

    operator fun getValue(thisT: Any, property: KProperty<*>): IJR? {
        val thisModelBuilder = thisT.buildInModelBuilder
        val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
        Scopes.setModelBuilder(ijModelBuilder)

        val allA = thisModelBuilder.allA.toSet() as Set<A>
        val thisA = thisModelBuilder.tToA[thisT]!! as A
        val aClazz = thisA::class

        val pd = IJPDesc(mapper)
        val rd = RDesc(property)

        // A->IJM
        // A->C[IJM]
        if (pd.eq(rd)) {
            return handleAToIjm(thisA)
        }
        // A->IJA->IJT: IJP=IJA;IJR=IJT
        if (!pd.isColl() && !rd.isColl() && pd.isA() && rd.isT()) {
            return handleAToIjaToIjt(allA, rd, ijModelBuilder, thisA)
        }
        // A->C[IJA]->C[IJT]: IJP=C[IJA];IPR=C[IJT]
        if (pd.isColl() && rd.isColl() && pd.isA() && rd.isT()) {
            return handleAToIjacToIjtc(allA, rd, ijModelBuilder, thisA)
        }
        // A->IJI->IJA: IJP=IJI;IJR=IJA
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isA()) {
            return handleAToIjiToIja(rd, aClazz, allA, pd, ijModelBuilder, thisA)
        }
        // A->C[IJI]->C[IJA]: IJP=C[IJI];IJR=C[IJA]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isA()) {
            return handleAToIjicToIjac(rd, aClazz, allA, pd, ijModelBuilder, thisA)
        }
        // A->IJI->IJA->IJT: IJP=IJI;IJR=IJT
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isT()) {
            return handleAToIjiToIjaToIjt(rd, aClazz, allA, pd, ijModelBuilder, thisA)
        }
        // A->C[IJI]->C[IJA]->C[IJT]: IJP=C[IJI];IJR=C[IJT]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isT()) {
            return handleAToIjicToIjacToIjtc(rd, aClazz, allA, pd, ijModelBuilder, thisA)
        }
        err("cannot handle. mapper:$mapper. thisT:$thisT. property:$property")
    }

    // A->C[IJI]->C[IJA]->C[IJT]: IJP=C[IJI];IJR=C[IJT]
    private fun handleAToIjicToIjacToIjtc(
        rd: RDesc,
        aClazz: KClass<out A>,
        allA: Set<A>,
        pd: IJPDesc<A, IJP>,
        ijModelBuilder: ModelBuilder,
        thisA: A
    ): IJR {
        val ijtClazz = getT(rd.cType!!)!!
        val ijaClazz = tToAHolder[ijtClazz]!! as KClass<Any>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)

        val cached = ijModelBuilder.getIToTCache(ijtClazz)
            .filterKeys { i -> ijis.contains(i) }
        val unCachedIs = ijis.filter { !cached.keys.contains(it) }

        val ijiToIjt = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            ijModelBuilder buildMulti ijtClazz by unCachedIs
            val buildIjiToIjt = ijModelBuilder.iToT
            ijiToIjt += buildIjiToIjt
        }

        val currIjic = mapper.invoke(thisA)
        val thisIjtc = (currIjic as Collection<Any>).map { iji -> ijiToIjt[iji] }
        if (rd.isSet()) {
            return thisIjtc.toSet() as IJR
        }
        if (rd.isList()) {
            return thisIjtc.toList() as IJR
        }
        return thisIjtc as IJR
    }

    // A->IJI->IJA->IJT: IJP=IJI;IJR=IJT
    private fun handleAToIjiToIjaToIjt(
        rd: RDesc,
        aClazz: KClass<out A>,
        allA: Set<A>,
        pd: IJPDesc<A, IJP>,
        ijModelBuilder: ModelBuilder,
        thisA: A
    ): IJR? {
        val ijtClazz = getT(rd.type)!!
        val ijaClazz = tToAHolder[ijtClazz]!! as KClass<Any>
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)

        val cached = ijModelBuilder.getIToTCache(ijtClazz)
            .filterKeys { i -> ijis.contains(i) } as Map<IJP, IJR>
        val unCachedIs = ijis.filter { !cached.keys.contains(it) }

        val ijiToIjt = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            ijModelBuilder buildMulti ijtClazz by unCachedIs
            val buildIjiToIjt = ijModelBuilder.iToT as Map<IJP, IJR>
            ijiToIjt += buildIjiToIjt
        }

        val currIji = mapper.invoke(thisA)
        return ijiToIjt[currIji]
    }

    // A->C[IJI]->C[IJA]: IJP=C[IJI];IJR=C[IJA]
    private fun handleAToIjicToIjac(
        rd: RDesc,
        aClazz: KClass<out A>,
        allA: Set<A>,
        pd: IJPDesc<A, IJP>,
        ijModelBuilder: ModelBuilder,
        thisA: A
    ): IJR {
        val ijaClazz = getA(rd.cType!!)!!
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)

        val cached = ijModelBuilder.getIToACache(ijaClazz)
            .filterKeys { i -> ijis.contains(i) }
        val unCachedIs = ijis.filter { !cached.keys.contains(it) }

        val ijiToIja = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val ijaBuilder = builderHolder[ijaClazz]
                    as (Collection<Any>) -> Map<Any, Any>
            val buildIjiToIja = ijaBuilder.invoke(unCachedIs)
            ijModelBuilder.putAllIToACache(ijaClazz, buildIjiToIja)
            ijiToIja += buildIjiToIja
        }

        val currIjic = mapper.invoke(thisA)
        val thisIjac = (currIjic as Collection<Any>).map { iji -> ijiToIja[iji] }
        if (rd.isSet()) {
            return thisIjac.toSet() as IJR
        }
        if (rd.isList()) {
            return thisIjac.toList() as IJR
        }
        return thisIjac as IJR
    }

    // A->IJI->IJA: IJP=IJI;IJR=IJA
    private fun handleAToIjiToIja(
        rd: RDesc,
        aClazz: KClass<out A>,
        allA: Set<A>,
        pd: IJPDesc<A, IJP>,
        ijModelBuilder: ModelBuilder,
        thisA: A
    ): IJR? {
        val ijaClazz = getA(rd.type)!!
        val ijis = extractIjis<IJP>(aClazz, ijaClazz, allA, pd)

        val cached = ijModelBuilder.getIToACache(ijaClazz)
            .filterKeys { i -> ijis.contains(i) } as Map<IJP, IJR>
        val unCachedIs = ijis.filter { !cached.keys.contains(it) }

        val ijiToIja = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val ijaBuilder = builderHolder[ijaClazz]
                    as (Collection<IJP>) -> Map<IJP, IJR>
            val buildIjiToIja = ijaBuilder.invoke(unCachedIs)
            ijModelBuilder.putAllIToACache(ijaClazz, buildIjiToIja)
            ijiToIja += buildIjiToIja
        }

        val currIji = mapper.invoke(thisA)
        return ijiToIja[currIji]
    }

    // A->C[IJA]->C[IJT]: IJP=C[IJA];IPR=C[IJT]
    private fun handleAToIjacToIjtc(
        allA: Set<A>,
        rd: RDesc,
        ijModelBuilder: ModelBuilder,
        thisA: A
    ): IJR {
        val aToIjac = allA.map { a -> a to mapper.invoke(a) }.toMap()
        val ijas = aToIjac.values.stream()
            .flatMap { ijac -> (ijac as Collection<*>).stream() }
            .filter(::nonNull).map { it!! }.collect(toSet()).toSet()
        val ijtClazz = getT(rd.cType!!)!!

        val cached = ijModelBuilder.getAToTCache(ijtClazz)
            .filterKeys { a -> ijas.contains(a) } as Map<Any, Any>
        val unCachedAs = ijas.filter { !cached.keys.contains(it) }

        val ijaToIjt = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedAs)) {
            ijModelBuilder buildMulti (ijtClazz) by unCachedAs
            ijaToIjt += ijModelBuilder.aToT
        }

        val thisIjac = aToIjac[thisA] as Collection<Any>
        val thisIjtc = thisIjac.map { ija -> ijaToIjt[ija] } as Collection<Any>
        if (rd.isSet()) {
            return thisIjtc.toSet() as IJR
        }
        if (rd.isList()) {
            return thisIjtc.toList() as IJR
        }
        return thisIjtc as IJR
    }

    // A->IJA->IJT: IJP=IJA;IJR=IJT
    private fun handleAToIjaToIjt(
        allA: Set<A>,
        rd: RDesc,
        ijModelBuilder: ModelBuilder,
        thisA: A
    ): IJR? {
        val aToIja = allA.map { a -> a to mapper.invoke(a) }.toMap()
        val ijas = aToIja.values.toSet()
        val ijtClazz = getT(rd.type)!!

        val cached = ijModelBuilder.getAToTCache(ijtClazz)
            .filterKeys { a -> ijas.contains(a) } as Map<IJP, IJR>
        val unCachedAs = ijas.filter { !cached.keys.contains(it) }

        val ijaToIjt = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedAs)) {
            ijModelBuilder buildMulti (ijtClazz) by unCachedAs
            ijaToIjt += ijModelBuilder.aToT as Map<IJP, IJR>
        }
        return ijaToIjt[aToIja[thisA]]
    }

    // A->IJM
    // A->C[IJM]
    private fun handleAToIjm(thisA: A): IJR? {
        return mapper.invoke(thisA) as IJR?
    }

    private fun <IJI> extractIjis(
        aClazz: KClass<out A>,
        ijaClazz: KClass<Any>,
        allA: Collection<A>,
        pd: IJPDesc<A, IJP>
    ): Collection<IJI> {

        val ijaClazzToSingleMappers = BuildContext.singleInJoinHolder
            .computeIfAbsent(aClazz) { ConcurrentHashMap() }
        val singleMappers = ijaClazzToSingleMappers.computeIfAbsent(
            ijaClazz) { CopyOnWriteArraySet() } as MutableSet<(A) -> IJI>
        if (!pd.isColl() && !singleMappers.contains(mapper as (A) -> IJI)) singleMappers.add(mapper)
        val singleAToIjis = allA.map { a ->
            a to singleMappers.map { mapper -> (mapper.invoke(a)) }.toSet()}.toMap()
        val singleIjis = singleAToIjis.values.flatten().toSet()

        val ijaClazzToMultiMappers = BuildContext.multiInJoinHolder
            .computeIfAbsent(aClazz) { ConcurrentHashMap() }
        val multiMappers = ijaClazzToMultiMappers.computeIfAbsent(
            ijaClazz) { CopyOnWriteArraySet()} as MutableSet<(A) -> Collection<IJI>>
        if (pd.isColl() && !multiMappers.contains(mapper as (A) -> Collection<IJI>)) multiMappers.add(mapper)
        val multiAToIjis = allA.map { a ->
            a to multiMappers.map { mapper -> (mapper.invoke(a)) } .flatten().toSet()}.toMap()
        val multiIjis = multiAToIjis.values.flatten().toSet()

        return singleIjis + multiIjis
    }

    companion object {
        fun <A: Any, IJP: Any, IJR: Any> inJoin(mapper: (A) -> IJP) =
            InJoinDelegate<A, IJP, IJR>(mapper)
    }
}