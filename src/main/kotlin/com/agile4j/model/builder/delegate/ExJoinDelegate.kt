package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.eJPDescHolder
import com.agile4j.model.builder.build.BuildContext.getAClazzByType
import com.agile4j.model.builder.build.BuildContext.getTClazzByType
import com.agile4j.model.builder.build.BuildContext.rDescHolder
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes
import com.agile4j.utils.util.CollectionUtil
import java.util.*
import java.util.stream.Collectors
import kotlin.reflect.KClass
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
    private val mapper: (Collection<I>) -> Map<I, EJP?>) {

    operator fun getValue(thisT: Any, property: KProperty<*>): EJR? {
        //println("mb exo1-----${System.nanoTime()}")
        val thisModelBuilder = thisT.buildInModelBuilder
        //println("mb exo11----${System.nanoTime()}")
        val thisA = thisModelBuilder.getCurrAByT(thisT)!! as A
        //println("mb exo12----${System.nanoTime()}")
        val thisI = thisModelBuilder.getCurrIByA(thisA)!! as I

        //println("mb exo2-----${System.nanoTime()}")
        //val pd = EJPDesc(mapper)
        val pd = eJPDescHolder.get(mapper as (Collection<Any>) -> Map<Any, Any?>) as EJPDesc<I, EJP>
        //println("mb exo21----${System.nanoTime()}")
        //val rd = RDesc(property)
        val rd = rDescHolder.get(property)!!

        //println("mb exo3-----${System.nanoTime()}")
        val pdIsColl = pd.isColl
        val rdIsColl = rd.isColl
        val pdEqRd = pd.eq(rd)
        val pdIsA = pd.isA
        val rdIsT = rd.isT
        val pdIsI = pd.isI
        val rdIsA = rd.isA

        //println("mb exo4-----${System.nanoTime()}")
        try {
            // C[I]->M[I,EJM]
            if (!pdIsColl && !rdIsColl && pdEqRd) {
                return handleIToEjm(thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJM]]
            if (pdIsColl && rdIsColl && pdEqRd) {
                return handleIToEjmc(thisModelBuilder, thisI)
            }
            // C[I]->M[I,EJA]->M[I,EJT]: EJP=EJA;EJR=EJT
            if (!pdIsColl && !rdIsColl && pdIsA && rdIsT) {
                return handleIToEjaToEjt(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJA];EJR=C[EJT]
            if (pdIsColl && rdIsColl && pdIsA && rdIsT) {
                return handleIToEjacToEjtc(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,EJI]->M[I,EJA]: EJP=EJI;EJR=EJA
            if (!pdIsColl && !rdIsColl && pdIsI && rdIsA) {
                return handleIToIjiToEja(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJI]]->M[I,C[EJA]]: EJP=C[EJI];EJR=C[EJA]
            if (pdIsColl && rdIsColl && pdIsI && rdIsA) {
                return handleIToEjicToEjac(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]: EJP=EJI;EJR=EJT
            if (!pdIsColl && !rdIsColl && pdIsI && rdIsT) {
                return handleIToEjiToEja(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
            if (pdIsColl && rdIsColl && pdIsI && rdIsT) {
                return handleIToEjicToEjacToEjtc(rd, thisModelBuilder, thisI)
            }
            err("cannot handle. mapper:$mapper. thisT:$thisT. property:$property")
        } finally {
            Scopes.setModelBuilder(null)
            //println("mb exo5-----${System.nanoTime()}")
        }

    }

    private fun putIToEjmCache(
        ejModelBuilder: ModelBuilder,
        mapper: (Collection<I>) -> Map<I, EJP?>,
        buildIToEjm: Map<I, EJP?>,
        unCachedIs: List<I>
        ) {
        val unFetchedIs = unCachedIs.filter { !buildIToEjm.keys.contains(it) }.toSet()
        val unFetchedIToEjm = unFetchedIs.map { it to null }.toMap()
        ejModelBuilder.putGlobalIToEjmCache(mapper, buildIToEjm)
        ejModelBuilder.putGlobalIToEjmCache(mapper, unFetchedIToEjm)
    }

    private fun <I, A: Any> putIToACache(
        modelBuilder: ModelBuilder,
        AClazz: KClass<A>,
        buildIToA: Map<I, A>,
        unCachedIs: List<I>
    ) {
        val unFetchedIs = unCachedIs.filter { !buildIToA.keys.contains(it) }.toSet()
        val unFetchedIToA = unFetchedIs.map { it to null }.toMap()
        modelBuilder.putGlobalIToACache(AClazz, buildIToA + unFetchedIToA)
    }

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
    private fun handleIToEjicToEjacToEjtc(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR {
        val ejtClazz = getTClazzByType(rd.cType!!)!!

        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)) as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEjic = (thisICache[thisI] as Collection<*>).stream()
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
            val thisEjicCache = thisModelBuilder.getGlobalIToTCache(ejtClazz, thisEjic)
            if (thisEjicCache.size == thisEjic.size) {
                val thisEjtc = thisEjicCache.values.stream()
                    .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
                if (rd.isSet) {
                    return thisEjtc.toSet() as EJR
                }
                if (rd.isList) {
                    return thisEjtc.toList() as EJR
                }
                return thisEjtc as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjic = thisModelBuilder.getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJP?>
        val unCachedIs = allI.filter { !iToEjic.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjic, unCachedIs)
            iToEjic += buildIToEjic
        }

        val ejis = iToEjic.values.stream()
            .flatMap { ejic -> (ejic as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
        val ejiToEjt = thisModelBuilder.getGlobalIToTCache(ejtClazz, ejis)
        val unCachedEjis = ejis.filter { !ejiToEjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjis
            ejiToEjt += ejModelBuilder.currIToT
        }

        val thisEjtc = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEjt[eji] }
        if (rd.isSet) {
            return thisEjtc.toSet() as EJR
        }
        if (rd.isList) {
            return thisEjtc.toList() as EJR
        }
        return thisEjtc as EJR
    }

    // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]: EJP=EJI;EJR=EJT
    private fun handleIToEjiToEja(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val ejtClazz = getTClazzByType(rd.type)!!

        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)) as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEji = thisICache[thisI]
            val thisEjiCache = thisModelBuilder
                .getGlobalIToTCache(ejtClazz, setOf(thisEji)) as MutableMap<EJP?, EJR>
            if (thisEjiCache.size == 1) {
                return thisEjiCache[thisEji]
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEji = thisModelBuilder
            .getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJP?>
        val unCachedIs = allI.filter { !iToEji.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEji = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEji, unCachedIs)
            iToEji += buildIToEji
        }

        val ejis = iToEji.values
        val ejiToEjt = thisModelBuilder
            .getGlobalIToTCache(ejtClazz, ejis) as MutableMap<EJP?, EJR>
        val unCachedEjis = ejis.filter { !ejiToEjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjis
            ejiToEjt += (ejModelBuilder.currIToT as Map<EJP?, EJR>)
        }

        return ejiToEjt[iToEji[thisI]]
    }

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]: EJP=C[EJI];EJR=C[EJA]
    private fun handleIToEjicToEjac(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR {
        val ejaClazz = getAClazzByType(rd.cType!!)!!

        val thisICache = thisModelBuilder.getGlobalIToEjmCache(mapper, setOf(thisI)) as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEjic = (thisICache[thisI] as Collection<*>).stream()
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
            val thisEjicCache = thisModelBuilder.getGlobalIToACache(ejaClazz, thisEjic)
            if (thisEjicCache.size == thisEjic.size) {
                val thisEjac = thisEjicCache.values.stream()
                    .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
                if (rd.isSet) {
                    return thisEjac.toSet() as EJR
                }
                if (rd.isList) {
                    return thisEjac.toList() as EJR
                }
                return thisEjac as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjic = thisModelBuilder
            .getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJP?>
        val unCachedIs = allI.filter { !iToEjic.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjic, unCachedIs)
            iToEjic += buildIToEjic
        }

        val ejis = iToEjic.values.stream()
            .flatMap { ejic -> (ejic as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
        val ejiToEja = thisModelBuilder.getGlobalIToACache(ejaClazz, ejis)
        val unCachedEjis = ejis.filter { !ejiToEja.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            val ejaBuilder = builderHolder[ejaClazz]
                    as (Collection<Any>) -> Map<Any, Any>
            val buildEjiToEja = ejaBuilder.invoke(unCachedEjis)
            putIToACache(thisModelBuilder, ejaClazz, buildEjiToEja, unCachedEjis)
            ejiToEja += buildEjiToEja
        }

        val thisEjac = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEja[eji] }
        if (rd.isSet) {
            return thisEjac.toSet() as EJR
        }
        if (rd.isList) {
            return thisEjac.toList() as EJR
        }
        return thisEjac as EJR
    }

    // C[I]->M[I,EJI]->M[I,EJA]: EJP=EJI;EJR=EJA
    private fun handleIToIjiToEja(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val ejaClazz = getAClazzByType(rd.type)!!

        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)) as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEji = thisICache[thisI]
            val thisEjiCache = thisModelBuilder
                .getGlobalIToACache(ejaClazz, setOf(thisEji)) as Map<EJP?, EJR>
            if (thisEjiCache.size == 1) {
                return thisEjiCache[thisEji]
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val cached = thisModelBuilder.getGlobalIToEjmCache(mapper, allI) as Map<I, EJP?>
        val unCachedIs = allI.filter { !cached.keys.contains(it) }

        val iToEji = cached.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEji = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEji, unCachedIs)
            iToEji += buildIToEji
        }

        val ejis = iToEji.values
        val ejiToEja = thisModelBuilder
            .getGlobalIToACache(ejaClazz, ejis) as MutableMap<EJP?, EJR>
        val unCachedEjis = ejis.filter { !ejiToEja.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedEjis)) {
            val ejaBuilder = builderHolder[ejaClazz]
                    as (Collection<EJP?>) -> Map<EJP?, EJR>
            val buildEjiToEja = ejaBuilder.invoke(unCachedEjis)
            putIToACache(thisModelBuilder, ejaClazz, buildEjiToEja, unCachedEjis)
            ejiToEja += buildEjiToEja
        }

        return ejiToEja[iToEji[thisI]]
    }

    // C[I]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJA];EJR=C[EJT]
    private fun handleIToEjacToEjtc(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR {
        val ejtClazz = getTClazzByType(rd.cType!!)!!

        val thisICache = thisModelBuilder.getGlobalIToEjmCache(mapper, setOf(thisI)) as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEjac = (thisICache[thisI] as Collection<*>).stream()
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
            val thisEjacCache = thisModelBuilder.getGlobalAToTCache(ejtClazz, thisEjac)
            if (thisEjacCache.size == thisEjac.size) {
                val thisEjtc = thisEjacCache.values.stream()
                    .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
                if (rd.isSet) {
                    return thisEjtc.toSet() as EJR
                }
                if (rd.isList) {
                    return thisEjtc.toList() as EJR
                }
                return thisEjtc as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjac = thisModelBuilder.getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJP?>
        val unCachedIs = allI.filter { !iToEjac.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjac = mapper.invoke(allI)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjac, unCachedIs)
            iToEjac += buildIToEjac
        }

        val ejas = iToEjac.values.stream()
            .flatMap { ejac -> (ejac as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
        val ejaToEjt = thisModelBuilder.getGlobalAToTCache(ejtClazz, ejas)
        val unCachedEjas = ejas.filter { !ejaToEjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedEjas)) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjas
            ejaToEjt += ejModelBuilder.currAToT
        }

        val thisEjac = iToEjac[thisI] as Collection<Any>
        val thisEjtc = thisEjac.map { eja -> ejaToEjt[eja] } as Collection<Any>
        if (rd.isSet) {
            return thisEjtc.toSet() as EJR
        }
        if (rd.isList) {
            return thisEjtc.toList() as EJR
        }
        return thisEjtc as EJR
    }

    // C[I]->M[I,EJA]->M[I,EJT]: EJP=EJA;EJR=EJT
    private fun handleIToEjaToEjt(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val ejtClazz = getTClazzByType(rd.type)!!

        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)) as MutableMap<I, EJP?>
        if (thisICache.size == 1) {
            val thisEja = thisICache[thisI]
            val thisEjaCache = thisModelBuilder
                .getGlobalAToTCache(ejtClazz, setOf(thisEja)) as MutableMap<EJP?, EJR>
            if (thisEjaCache.size == 1) {
                return thisEjaCache[thisEja]
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEja = thisModelBuilder
            .getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJP?>
        val unCachedIs = allI.filter { !iToEja.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEja = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEja, unCachedIs)
            iToEja += buildIToEja
        }

        val ejas = iToEja.values
        val ejaToEjt = thisModelBuilder
            .getGlobalAToTCache(ejtClazz, ejas) as MutableMap<EJP?, EJR>
        val unCachedEjas = ejas.filter { !ejaToEjt.keys.contains(it) }

        if (CollectionUtil.isNotEmpty(unCachedEjas)) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjas
            val buildEjaToEjt = ejModelBuilder.currAToT as Map<EJP?, EJR>
            ejaToEjt += buildEjaToEjt
        }

        return ejaToEjt[iToEja[thisI]]
    }

    // C[I]->M[I,C[EJM]]
    private fun handleIToEjmc(
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)) as MutableMap<I, EJR>
        if (thisICache.size == 1) {
            return thisICache[thisI]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val cachedIToEjm = thisModelBuilder
            .getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJR>
        val unCachedIs = allI.filter { !cachedIToEjm.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedIs)) return cachedIToEjm[thisI]

        val buildIToEjm = mapper.invoke(unCachedIs)
        putIToEjmCache(thisModelBuilder, mapper, buildIToEjm, unCachedIs)

        /*if (rd.isA() && MapUtil.isNotEmpty(buildIToEjm)) {
            val ejas = buildIToEjm.values.stream()
                .flatMap { ejac -> (ejac as Collection<*>).stream() }
                .filter(Objects::nonNull).map { it!! }
                .collect(Collectors.toSet())
            val aClazz = getA(rd.cType!!)!!
            val indexer = BuildContext.indexerHolder[aClazz] as (Any) -> Any
            val iToA = ejas.associateBy({indexer.invoke(it)}, {it})
            ejModelBuilder.putGlobalIToACache(aClazz, iToA)
        }*/

        return (buildIToEjm + cachedIToEjm)[thisI] as EJR?
    }

    // C[I]->M[I,EJM]
    private fun handleIToEjm(
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)) as MutableMap<I, EJR>
        if (thisICache.size == 1) {
            return thisICache[thisI]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val cachedIToEjm = thisModelBuilder
            .getGlobalIToEjmCache(mapper, allI) as MutableMap<I, EJR>
        val unCachedIs = allI.filter { !cachedIToEjm.keys.contains(it) }
        if (CollectionUtil.isEmpty(unCachedIs)) return cachedIToEjm[thisI]

        val buildIToEjm = mapper.invoke(unCachedIs)
        putIToEjmCache(thisModelBuilder, mapper, buildIToEjm, unCachedIs)

        /*if (rd.isA() && MapUtil.isNotEmpty(buildIToEjm)) {
            val ejas = buildIToEjm.values
            val aClazz = getA(rd.type)!!
            val indexer = BuildContext.indexerHolder[aClazz] as (Any?) -> Any?
            val iToA = ejas.associateBy({indexer.invoke(it)}, {it})
            ejModelBuilder.putGlobalIToACache(aClazz, iToA)
        }*/

        return (buildIToEjm + cachedIToEjm)[thisI] as EJR?
    }

    companion object {
        fun <I: Any, EJP: Any, EJR: Any> exJoin(mapper: (Collection<I>) -> Map<I, EJP?>) =
            ExJoinDelegate<I, Any, EJP, EJR>(mapper)
    }
}