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
        val thisModelBuilder = thisT.buildInModelBuilder
        val thisA = thisModelBuilder.getCurrAByT(thisT)!! as A
        val thisI = thisModelBuilder.getCurrIByA(thisA)!! as I

        val pd = eJPDescHolder.get(mapper as (Collection<Any>) -> Map<Any, Any?>) as EJPDesc<I, EJP>
        val rd = rDescHolder.get(property)!!

        val pdEqRd = pd.eq(rd)
        try {
            // C[I]->M[I,EJM]
            if (!pd.isColl && !rd.isColl && pdEqRd) {
                return handleIToEjm(thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJM]]
            if (pd.isColl && rd.isColl && pdEqRd) {
                return handleIToEjmc(thisModelBuilder, thisI)
            }
            // C[I]->M[I,EJA]->M[I,EJT]: EJP=EJA;EJR=EJT
            if (!pd.isColl && !rd.isColl && pd.isA && rd.isT) {
                return handleIToEjaToEjt(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJA];EJR=C[EJT]
            if (pd.isColl && rd.isColl && pd.isA && rd.isT) {
                return handleIToEjacToEjtc(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,EJI]->M[I,EJA]: EJP=EJI;EJR=EJA
            if (!pd.isColl && !rd.isColl && pd.isI && rd.isA) {
                return handleIToIjiToEja(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJI]]->M[I,C[EJA]]: EJP=C[EJI];EJR=C[EJA]
            if (pd.isColl && rd.isColl && pd.isI && rd.isA) {
                return handleIToEjicToEjac(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]: EJP=EJI;EJR=EJT
            if (!pd.isColl && !rd.isColl && pd.isI && rd.isT) {
                return handleIToEjiToEja(rd, thisModelBuilder, thisI)
            }
            // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
            if (pd.isColl && rd.isColl && pd.isI && rd.isT) {
                return handleIToEjicToEjacToEjtc(rd, thisModelBuilder, thisI)
            }
            err("cannot handle. mapper:$mapper. thisT:$thisT. property:$property")
        } finally {
            Scopes.setModelBuilder(null)
        }

    }

    private fun putIToEjmCache(
        ejModelBuilder: ModelBuilder,
        mapper: (Collection<I>) -> Map<I, EJP?>,
        buildIToEjm: Map<I, EJP?>,
        unCachedIs: Collection<I>
        ) {
        ejModelBuilder.putGlobalIToEjmCache(mapper, buildIToEjm)
        if (buildIToEjm.size == unCachedIs.size) return

        val unFetchedIs = unCachedIs.filter { !buildIToEjm.keys.contains(it) }.toSet()
        val unFetchedIToEjm = unFetchedIs.associateWith { null }
        ejModelBuilder.putGlobalIToEjmCache(mapper, unFetchedIToEjm)
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

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
    private fun handleIToEjicToEjacToEjtc(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR {
        val ejtClazz = getTClazzByType(rd.cType!!)!!

        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEjic = (thisICache[thisI] as Collection<*>).stream()
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())
            val cacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, thisEjic)
            val thisEjicCache = cacheResp.cached
            if (thisEjicCache.size == thisEjic.size) {
                val thisEjtc = thisEjicCache.values.stream()
                    .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())
                if (rd.isSet) {
                    return thisEjtc.toSet() as EJR
                }
                if (rd.isList) {
                    return thisEjtc as EJR
                }
                return thisEjtc as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjicCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val iToEjic = iToEjicCacheResp.cached as MutableMap<I, EJP?>
        val unCachedIs = iToEjicCacheResp.unCached as Collection<I>

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjic, unCachedIs)
            iToEjic += buildIToEjic
        }

        val ejis = iToEjic.values.stream()
            .flatMap { ejic -> (ejic as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
        val cacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, ejis)
        val ejiToEjt = cacheResp.cached
        val unCachedEjis = cacheResp.unCached as Collection<Any>

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
            return thisEjtc as EJR
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
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEji = thisICache[thisI]
            val cacheResp = thisModelBuilder
                .getGlobalIToTCache(ejtClazz, setOf(thisEji))
            val thisEjiCache = cacheResp.cached as MutableMap<EJP?, EJR>
            if (thisEjiCache.size == 1) {
                return thisEjiCache[thisEji]
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjiCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val iToEji = iToEjiCacheResp.cached as MutableMap<I, EJP?>
        val unCachedIs = iToEjiCacheResp.unCached as Collection<I>

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEji = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEji, unCachedIs)
            iToEji += buildIToEji
        }

        val ejis = iToEji.values
        val cacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, ejis)
        val ejiToEjt = cacheResp.cached as MutableMap<EJP?, EJR>
        val unCachedEjis = cacheResp.unCached as Collection<EJP?>

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

        val thisICache = thisModelBuilder.getGlobalIToEjmCache(mapper, setOf(thisI)).cached as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEjic = (thisICache[thisI] as Collection<*>).stream()
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())
            val cacheResp = thisModelBuilder.getGlobalIToACache(ejaClazz, thisEjic)
            val thisEjicCache = cacheResp.cached
            if (thisEjicCache.size == thisEjic.size) {
                val thisEjac = thisEjicCache.values.stream()
                    .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())
                if (rd.isSet) {
                    return thisEjac.toSet() as EJR
                }
                if (rd.isList) {
                    return thisEjac as EJR
                }
                return thisEjac as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjicCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val iToEjic = iToEjicCacheResp.cached as MutableMap<I, EJP?>
        val unCachedIs = iToEjicCacheResp.unCached as Collection<I>

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjic, unCachedIs)
            iToEjic += buildIToEjic
        }

        val ejis = iToEjic.values.stream()
            .flatMap { ejic -> (ejic as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
        val cacheResp = thisModelBuilder.getGlobalIToACache(ejaClazz, ejis)
        val ejiToEja = cacheResp.cached
        val unCachedEjis = cacheResp.unCached as Collection<Any>

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
            return thisEjac as EJR
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
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEji = thisICache[thisI]
            val cacheResp = thisModelBuilder
                .getGlobalIToACache(ejaClazz, setOf(thisEji))
            val thisEjiCache = cacheResp.cached as Map<EJP?, EJR>
            if (thisEjiCache.size == 1) {
                return thisEjiCache[thisEji]
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val cachedI = iCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iCacheResp.unCached as Collection<I>

        val iToEji = cachedI.toMutableMap()
        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEji = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEji, unCachedIs)
            iToEji += buildIToEji
        }

        val ejis = iToEji.values
        val cacheResp = thisModelBuilder
            .getGlobalIToACache(ejaClazz, ejis)
        val ejiToEja = cacheResp.cached as MutableMap<EJP?, EJR>
        val unCachedEjis = cacheResp.unCached as Collection<EJP?>

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

        val thisICache = thisModelBuilder.getGlobalIToEjmCache(mapper, setOf(thisI)).cached as Map<I, EJP?>
        if (thisICache.size == 1) {
            val thisEjac = (thisICache[thisI] as Collection<*>).stream()
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())
            val cacheResp = thisModelBuilder.getGlobalAToTCache(ejtClazz, thisEjac)
            val thisEjacCache = cacheResp.cached
            if (thisEjacCache.size == thisEjac.size) {
                val thisEjtc = thisEjacCache.values.stream()
                    .filter(Objects::nonNull).map { it!! }.collect(Collectors.toList())
                if (rd.isSet) {
                    return thisEjtc.toSet() as EJR
                }
                if (rd.isList) {
                    return thisEjtc as EJR
                }
                return thisEjtc as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjacCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val iToEjac = iToEjacCacheResp.cached as MutableMap<I, EJP?>
        val unCachedIs = iToEjacCacheResp.unCached as Collection<I>

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEjac = mapper.invoke(allI)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjac, unCachedIs)
            iToEjac += buildIToEjac
        }

        val ejas = iToEjac.values.stream()
            .flatMap { ejac -> (ejac as Collection<*>).stream() }
            .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet())
        val cacheResp  = thisModelBuilder.getGlobalAToTCache(ejtClazz, ejas)
        val ejaToEjt = cacheResp.cached
        val unCachedEjas = cacheResp.unCached as Collection<Any>

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
            return thisEjtc as EJR
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
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as MutableMap<I, EJP?>
        if (thisICache.size == 1) {
            val thisEja = thisICache[thisI]
            val cacheResp = thisModelBuilder
                .getGlobalAToTCache(ejtClazz, setOf(thisEja))
            val thisEjaCache = cacheResp.cached as MutableMap<EJP?, EJR>
            if (thisEjaCache.size == 1) {
                return thisEjaCache[thisEja]
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjaCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val iToEja = iToEjaCacheResp.cached as MutableMap<I, EJP?>
        val unCachedIs = iToEjaCacheResp.unCached as Collection<I>

        if (CollectionUtil.isNotEmpty(unCachedIs)) {
            val buildIToEja = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEja, unCachedIs)
            iToEja += buildIToEja
        }

        val ejas = iToEja.values
        val cacheResp = thisModelBuilder.getGlobalAToTCache(ejtClazz, ejas)
        val ejaToEjt = cacheResp.cached as MutableMap<EJP?, EJR>
        val unCachedEjas = cacheResp.unCached as Collection<EJP?>

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
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as MutableMap<I, EJR>
        if (thisICache.size == 1) {
            return thisICache[thisI]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val cachedIToEjmCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val cachedIToEjm = cachedIToEjmCacheResp.cached as MutableMap<I, EJR>
        val unCachedIs = cachedIToEjmCacheResp.unCached as Collection<I>
        if (CollectionUtil.isEmpty(unCachedIs)) return cachedIToEjm[thisI]

        val buildIToEjm = mapper.invoke(unCachedIs)
        putIToEjmCache(thisModelBuilder, mapper, buildIToEjm, unCachedIs)

        return (buildIToEjm + cachedIToEjm)[thisI] as EJR?
    }

    // C[I]->M[I,EJM]
    private fun handleIToEjm(
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as MutableMap<I, EJR>
        if (thisICache.size == 1) {
            return thisICache[thisI]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val cachedIToEjmCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        val cachedIToEjm = cachedIToEjmCacheResp.cached as MutableMap<I, EJR>
        val unCachedIs = cachedIToEjmCacheResp.unCached as Collection<I>
        if (CollectionUtil.isEmpty(unCachedIs)) return cachedIToEjm[thisI]

        val buildIToEjm = mapper.invoke(unCachedIs)
        putIToEjmCache(thisModelBuilder, mapper, buildIToEjm, unCachedIs)

        return (buildIToEjm + cachedIToEjm)[thisI] as EJR?
    }

    companion object {
        fun <I: Any, EJP: Any, EJR: Any> exJoin(mapper: (Collection<I>) -> Map<I, EJP?>) =
            ExJoinDelegate<I, Any, EJP, EJR>(mapper)
    }
}