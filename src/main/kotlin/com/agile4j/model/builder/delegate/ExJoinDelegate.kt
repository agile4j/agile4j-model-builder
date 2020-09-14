package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getA
import com.agile4j.model.builder.build.BuildContext.getT
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.util.*
import java.util.stream.Collectors
import kotlin.reflect.KProperty

/**
 * abbreviations:
 * I        index
 * A        accompany
 * T        target
 * C        collection
 * EJP      exJoinProvide
 * EJR      exJoinRequire
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class ExJoinDelegate<I: Any, A:Any, EJP: Any, EJR: Any>(
    private val mapper: (Collection<I>) -> Map<I, EJP?>) /*: JoinDelegate<EJR>*/ {

    operator fun getValue(thisT: Any, property: KProperty<*>): EJR? {
        val thisModelBuilder = thisT.buildInModelBuilder
        val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
        Scopes.setModelBuilder(ejModelBuilder)

        val allI = thisModelBuilder.allI as Set<I>
        val thisA = thisModelBuilder.tToA[thisT]!! as A
        val thisI = thisModelBuilder.aToI[thisA] as I

        val pd = EJPDesc(mapper)
        val rd = RDesc(property)

        try {
            // C[I]->M[I,EJM]
            if (!pd.isColl() && !rd.isColl() && pd.eq(rd)) {
                return handleIToEjm(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,C[EJM]]
            if (pd.isColl() && rd.isColl() && pd.eq(rd)) {
                return handleIToEjmc(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,EJA]->M[I,EJT]: EJP=EJA;EJR=EJT
            if (!pd.isColl() && !rd.isColl() && pd.isA() && rd.isT()) {
                return handleIToEjaToEjt(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJA];EJR=C[EJT]
            if (pd.isColl() && rd.isColl() && pd.isA() && rd.isT()) {
                return handleIToEjacToEjtc(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,EJI]->M[I,EJA]: EJP=EJI;EJR=EJA
            if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isA()) {
                return handleIToIjiToEja(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,C[EJI]]->M[I,C[EJA]]: EJP=C[EJI];EJR=C[EJA]
            if (pd.isColl() && rd.isColl() && pd.isI() && rd.isA()) {
                return handleIToEjicToEjac(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]: EJP=EJI;EJR=EJT
            if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isT()) {
                return handleIToEjiToEja(ejModelBuilder, allI, rd, thisI)
            }
            // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
            if (pd.isColl() && rd.isColl() && pd.isI() && rd.isT()) {
                return handleIToEjicToEjacToEjtc(ejModelBuilder, allI, rd, thisI)
            }
            err("cannot handle. mapper:$mapper. thisT:$thisT. property:$property")
        } finally {
            Scopes.setModelBuilder(null)
        }

    }

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
    private fun handleIToEjicToEjacToEjtc(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP?>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEjic = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            ejModelBuilder.putAllIToEjmCache(mapper, buildIToEjic)
            iToEjic += buildIToEjic
        }

        val ejis = iToEjic.values.stream()
            .flatMap { ejic -> (ejic as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet()).toSet()
        val ejtClazz = getT(rd.cType!!)!!

        val cachedEjiToEjt = ejModelBuilder.getIToTCache(ejtClazz)
        val unCachedEjis = ejis.filter { !cachedEjiToEjt.keys.contains(it) }

        val ejiToEjt = cachedEjiToEjt.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            ejModelBuilder buildMulti ejtClazz by unCachedEjis
            ejiToEjt += ejModelBuilder.iToT
        }

        val thisEjtc = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEjt[eji] }
        if (rd.isSet()) {
            return thisEjtc.toSet() as EJR
        }
        if (rd.isList()) {
            return thisEjtc.toList() as EJR
        }
        return thisEjtc as EJR
    }

    // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]: EJP=EJI;EJR=EJT
    private fun handleIToEjiToEja(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR? {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP?>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEji = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEji = mapper.invoke(unCachedIs)
            ejModelBuilder.putAllIToEjmCache(mapper, buildIToEji)
            iToEji += buildIToEji
        }

        val ejis = iToEji.values.toSet()
        val ejtClazz = getT(rd.type)!!

        val cachedEjiToEjt = ejModelBuilder.getIToTCache(ejtClazz)
        val unCachedEjis = ejis.filter { !cachedEjiToEjt.keys.contains(it) }

        val ejiToEjt = cachedEjiToEjt.toMutableMap() as MutableMap<EJP?, EJR>
        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            ejModelBuilder buildMulti ejtClazz by unCachedEjis
            ejiToEjt += (ejModelBuilder.iToT as Map<EJP?, EJR>)
        }

        return ejiToEjt[iToEji[thisI]]
    }

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]: EJP=C[EJI];EJR=C[EJA]
    private fun handleIToEjicToEjac(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP?>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEjic = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            ejModelBuilder.putAllIToEjmCache(mapper, buildIToEjic)
            iToEjic += buildIToEjic
        }

        val ejis = iToEjic.values.stream()
            .flatMap { ejic -> (ejic as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet()).toSet()
        val ejaClazz = getA(rd.cType!!)!!

        val cachedEjiToEja = ejModelBuilder.getIToACache(ejaClazz)
        val unCachedEjis = ejis.filter { !cachedEjiToEja.keys.contains(it) }

        val ejiToEja = cachedEjiToEja.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            val ejaBuilder = builderHolder[ejaClazz]
                    as (Collection<Any>) -> Map<Any, Any>
            val buildEjiToEja = ejaBuilder.invoke(unCachedEjis)
            ejModelBuilder.putAllIToACache(ejaClazz, buildEjiToEja)
            ejiToEja += buildEjiToEja
        }

        val thisEjac = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEja[eji] }
        if (rd.isSet()) {
            return thisEjac.toSet() as EJR
        }
        if (rd.isList()) {
            return thisEjac.toList() as EJR
        }
        return thisEjac as EJR
    }

    // C[I]->M[I,EJI]->M[I,EJA]: EJP=EJI;EJR=EJA
    private fun handleIToIjiToEja(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR? {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP?>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEji = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEji = mapper.invoke(unCachedIs)
            ejModelBuilder.putAllIToEjmCache(mapper, buildIToEji)
            iToEji += buildIToEji
        }

        val ejis = iToEji.values.toSet()
        val ejaClazz = getA(rd.type)!!


        val cachedEjiToEja = (ejModelBuilder.getIToACache(ejaClazz))
            .filterKeys { ejis.contains(it) } as Map<EJP?, EJR>
        val unCachedEjiToEja = ejis.filter { !cachedEjiToEja.keys.contains(it) }

        val ejiToEja = cachedEjiToEja.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedEjiToEja)) {
            val ejaBuilder = builderHolder[ejaClazz]
                    as (Collection<EJP?>) -> Map<EJP?, EJR>
            val buildEjiToEja = ejaBuilder.invoke(ejis)
            ejModelBuilder.putAllIToACache(ejaClazz, buildEjiToEja)
            ejiToEja += buildEjiToEja
        }

        return ejiToEja[iToEji[thisI]]
    }

    // C[I]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJA];EJR=C[EJT]
    private fun handleIToEjacToEjtc(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP?>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEjac = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjac = mapper.invoke(allI)
            ejModelBuilder.putAllIToEjmCache(mapper, buildIToEjac)
            iToEjac += buildIToEjac
        }

        val ejas = iToEjac.values.stream()
            .flatMap { ejac -> (ejac as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet()).toSet()
        val ejtClazz = getT(rd.cType!!)!!

        val cachedEjaToEjt = ejModelBuilder.getAToTCache(ejtClazz)
            .filterKeys { ejas.contains(it) }
        val unCachedEjas = ejas.filter { !cachedEjaToEjt.keys.contains(it) }

        val ejaToEjt = cachedEjaToEjt.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedEjas)) {
            ejModelBuilder buildMulti ejtClazz by unCachedEjas
            ejaToEjt += ejModelBuilder.aToT
        }

        val thisEjac = iToEjac[thisI] as Collection<Any>
        val thisEjtc = thisEjac.map { eja -> ejaToEjt[eja] } as Collection<Any>
        if (rd.isSet()) {
            return thisEjtc.toSet() as EJR
        }
        if (rd.isList()) {
            return thisEjtc.toList() as EJR
        }
        return thisEjtc as EJR
    }

    // C[I]->M[I,EJA]->M[I,EJT]: EJP=EJA;EJR=EJT
    private fun handleIToEjaToEjt(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR? {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP?>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEja = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEja = mapper.invoke(unCachedIs)
            ejModelBuilder.putAllIToEjmCache(mapper, buildIToEja)
            iToEja += buildIToEja
        }

        val ejas = iToEja.values.toSet()
        val ejtClazz = getT(rd.type)!!

        val cachedEjaToEjt = (ejModelBuilder.getAToTCache(ejtClazz) as Map<EJP?, EJR>)
            .filterKeys { ejas.contains(it) }
        val unCachedEjas = ejas.filter { !cachedEjaToEjt.keys.contains(it) }

        val ejaToEjt = cachedEjaToEjt.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedEjas)) {
            ejModelBuilder buildMulti ejtClazz by unCachedEjas
            val buildEjaToEjt = ejModelBuilder.aToT as Map<EJP?, EJR>
            ejaToEjt += buildEjaToEjt
        }

        return ejaToEjt[iToEja[thisI]]
    }

    // C[I]->M[I,EJM]
    private fun handleIToEjm(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR? {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJR>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedIs)) return cached[thisI]

        val iToEjm = mapper.invoke(unCachedIs)
        ejModelBuilder.putAllIToEjmCache(mapper, iToEjm)
        if (rd.isA() && MapUtil.isNotEmpty(iToEjm)) {
            val ejas = iToEjm.values
            val aClazz = getA(rd.type)!!
            val indexer = BuildContext.indexerHolder[aClazz] as (Any?) -> Any?
            val iToA = ejas.map{indexer.invoke(it) to it}.toMap()
            ejModelBuilder.putAllIToACache(aClazz, iToA)
        }

        return (iToEjm + cached)[thisI] as EJR?
    }

    // C[I]->M[I,C[EJM]]
    private fun handleIToEjmc(
        ejModelBuilder: ModelBuilder,
        allI: Set<I>,
        rd: RDesc,
        thisI: I
    ): EJR? {
        val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJR>)
            .filterKeys { allI.contains(it) }
        val unCachedIs = allI.filter { !cached.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedIs)) return cached[thisI]

        val iToEjm = mapper.invoke(unCachedIs)
        ejModelBuilder.putAllIToEjmCache(mapper, iToEjm)
        if (rd.isA() && MapUtil.isNotEmpty(iToEjm)) {
            val ejas = iToEjm.values.stream()
                .flatMap { ejac -> (ejac as Collection<*>).stream() }
                .filter(Objects::nonNull).map { it!! }
                .collect(Collectors.toSet()).toSet()
            val aClazz = getA(rd.cType!!)!!
            val indexer = BuildContext.indexerHolder[aClazz] as (Any) -> Any
            val iToA = ejas.map{indexer.invoke(it) to it}.toMap()
            ejModelBuilder.putAllIToACache(aClazz, iToA)
        }

        return (iToEjm + cached)[thisI] as EJR?
    }

    companion object {
        fun <I: Any, EJP: Any, EJR: Any> exJoin(mapper: (Collection<I>) -> Map<I, EJP?>) =
            ExJoinDelegate<I, Any, EJP, EJR>(mapper)
    }
}