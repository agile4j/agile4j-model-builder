package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getA
import com.agile4j.model.builder.build.BuildContext.getT
import com.agile4j.model.builder.build.BuildContext.tToAHolder
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.utils.util.CollectionUtil
import java.util.Objects.nonNull
import java.util.stream.Collectors.toSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * 注意：设计理念上，IJ，mapper: (A->IJP),是不需要成本的，即不需要缓存，从model自身即可获取
 * 如果需要成本，那应该用EJ，而非IJ
 * TODO 反向的先不处理，以后再说。例如：A->IJA->IJI
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class InternalJoinDelegate<A: Any, IJP: Any, IJR: Any>(private val mapper: (A) -> IJP) /*: JoinDelegate<IJR>*/ {

    operator fun getValue(thisT: Any, property: KProperty<*>): IJR? {
        val thisModelBuilder = thisT.buildInModelBuilder
        val ijModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
        JoinDelegate.ScopeKeys.setModelBuilder(ijModelBuilder)

        val allA = thisModelBuilder.allA.toSet() as Set<A>
        val thisA = thisModelBuilder.tToA[thisT]!! as A
        val aClazz = thisA::class

        val pd = IJPDesc(mapper)
        val rd = RDesc(property)

        // A->IJM
        // A->C[IJM]
        /*if (pd.eq(rd)) {
            // TODO cache? 判断下ijr的类型，如果是t/r，则入缓存，不过不利用缓存
            // TODO print warn
            return mapper.invoke(thisA) as IJR?
        }*/

        // TODO: all need cache
        
        // A->IJA->IJT: IJP=IJA;IJR=IJT
        if (!pd.isColl() && !rd.isColl() && pd.isA() && rd.isT()) {
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

        // A->C[IJA]->C[IJT]: IJP=C[IJA];IPR=C[IJT]
        if (pd.isColl() && rd.isColl() && pd.isA() && rd.isT()) {
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

        // A->IJI->IJA: IJP=IJI;IJR=IJA
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isA()) {

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

        // A->C[IJI]->C[IJA]: IJP=C[IJI];IJR=C[IJA]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isA()) {
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

        // A->IJI->IJA->IJT: IJP=IJI;IJR=IJT
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isT()) {
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

        // A->C[IJI]->C[IJA]->C[IJT]: IJP=C[IJI];IJR=C[IJT]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isT()) {
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

        err("cannot handle")
    }

    private fun <IJI> extractIjis(
        aClazz: KClass<out A>,
        ijaClazz: KClass<Any>,
        allA: Collection<A>,
        pd: IJPDesc<A, IJP>
    ): Collection<IJI> {

        val ijaClazzToSingleMappers = BuildContext.singleInJoinHolder
            .computeIfAbsent(aClazz) { mutableMapOf() }
        val singleMappers = ijaClazzToSingleMappers.computeIfAbsent(
            ijaClazz) { mutableSetOf()} as MutableSet<(A) -> IJI>
        if (!pd.isColl()) singleMappers.add(mapper as (A) -> IJI)
        val singleAToIjis = allA.map { a ->
            a to singleMappers.map { mapper -> (mapper.invoke(a)) }.toSet()}.toMap()
        val singleIjis = singleAToIjis.values.flatten().toSet()

        val ijaClazzToMultiMappers = BuildContext.multiInJoinHolder
            .computeIfAbsent(aClazz) { mutableMapOf() }
        val multiMappers = ijaClazzToMultiMappers.computeIfAbsent(
            ijaClazz) { mutableSetOf()} as MutableSet<(A) -> Collection<IJI>>
        if (pd.isColl()) multiMappers.add(mapper as (A) -> Collection<IJI>)
        val multiAToIjis = allA.map { a ->
            a to multiMappers.map { mapper -> (mapper.invoke(a)) } .flatten().toSet()}.toMap()
        val multiIjis = multiAToIjis.values.flatten().toSet()

        return singleIjis + multiIjis
    }

    companion object {
        fun <A: Any, IJP: Any, IJR: Any> inJoin(mapper: (A) -> IJP) =
            InternalJoinDelegate<A, IJP, IJR>(mapper)
    }
}