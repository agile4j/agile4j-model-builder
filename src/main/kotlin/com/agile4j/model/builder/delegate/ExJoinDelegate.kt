package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext.getAClazzByType
import com.agile4j.model.builder.build.BuildContext.getBuilder
import com.agile4j.model.builder.build.BuildContext.getEJPDesc
import com.agile4j.model.builder.build.BuildContext.getRDesc
import com.agile4j.model.builder.build.BuildContext.getTClazzByType
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.scope.Scopes
import com.agile4j.model.builder.utils.flatAndFilterNonNull
import com.agile4j.model.builder.utils.merge
import com.agile4j.model.builder.utils.parseColl
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

        val pd = getEJPDesc(mapper)
        val rd = getRDesc(property)

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

    // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
    private fun handleIToEjicToEjacToEjtc(
        rd: RDesc,
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR {
        val ejtClazz = getTClazzByType(rd.cType!!)!!

        val thisIToEjicCached = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as Map<I, EJP?>
        if (thisIToEjicCached.size == 1) {
            val thisEjic = (thisIToEjicCached[thisI] as Collection<*>).filter { it != null } as List<Any>
            val thisEjicToEjtcCacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, thisEjic)
            val thisEjicToEjtcCached = thisEjicToEjtcCacheResp.cached
            if (thisEjicToEjtcCached.size == thisEjic.size) {
                val thisEjtc = thisEjicToEjtcCached.values.filter { it != null } as List<Any>
                return parseColl(thisEjtc, rd) as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjicCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEjic = iToEjicCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iToEjicCacheResp.unCached as Collection<I>

        if (unCachedIs.isNotEmpty()) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjic, unCachedIs)
            iToEjic = merge(iToEjic as MutableMap<I, EJP?>, buildIToEjic)
        }

        val ejic = flatAndFilterNonNull(iToEjic.values)
        val ejicToEjtCacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, ejic)
        var ejiToEjt: Map<Any?, Any?> = ejicToEjtCacheResp.cached
        val unCachedEjis = ejicToEjtCacheResp.unCached as Collection<Any>

        if (unCachedEjis.isNotEmpty()) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjis
            val currIToT = ejModelBuilder.currIToT
            ejiToEjt = merge(ejiToEjt as MutableMap<Any?, Any?>, currIToT as MutableMap<Any?, Any?>)
        }

        val thisEjtc = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEjt[eji] }
        return parseColl(thisEjtc, rd) as EJR
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
            val thisEjiToEjtCacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, setOf(thisEji))
            val thisEjiToEjtCached = thisEjiToEjtCacheResp.cached as MutableMap<EJP?, EJR>
            if (thisEjiToEjtCached.size == 1) return thisEjiToEjtCached[thisEji]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjiCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEji = iToEjiCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iToEjiCacheResp.unCached as Collection<I>

        if (unCachedIs.isNotEmpty()) {
            val buildIToEji = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEji, unCachedIs)
            iToEji = merge(iToEji as MutableMap<I, EJP?>, buildIToEji)
        }

        val ejis = iToEji.values
        val cacheResp = thisModelBuilder.getGlobalIToTCache(ejtClazz, ejis)
        var ejiToEjt = cacheResp.cached as Map<EJP?, EJR>
        val unCachedEjis = cacheResp.unCached as Collection<EJP?>

        if (unCachedEjis.isNotEmpty()) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjis
            ejiToEjt = merge(ejiToEjt as MutableMap<EJP?, EJR>, ejModelBuilder.currIToT as Map<EJP?, EJR>)
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
            val thisEjic = (thisICache[thisI] as Collection<*>).filter { it != null } as List<Any>
            val thisEjicToEjacCacheResp = thisModelBuilder.getGlobalIToACache(ejaClazz, thisEjic)
            val thisEjicToEjacCached = thisEjicToEjacCacheResp.cached
            if (thisEjicToEjacCached.size == thisEjic.size) {
                val thisEjac = thisEjicToEjacCached.values.filter { it != null } as List<Any>
                return parseColl(thisEjac, rd) as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjicCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEjic = iToEjicCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iToEjicCacheResp.unCached as Collection<I>

        if (unCachedIs.isNotEmpty()) {
            val buildIToEjic = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjic, unCachedIs)
            iToEjic = merge(iToEjic as MutableMap<I, EJP?>, buildIToEjic)
        }

        val ejis = flatAndFilterNonNull(iToEjic.values)
        val ejiToEjaCacheResp = thisModelBuilder.getGlobalIToACache(ejaClazz, ejis)
        var ejiToEja: Map<Any?, Any?> = ejiToEjaCacheResp.cached
        val unCachedEjis = ejiToEjaCacheResp.unCached as Collection<Any>

        if (unCachedEjis.isNotEmpty()) {
            val ejaBuilder = getBuilder<Any, Any>(ejaClazz)
            val buildEjiToEja = ejaBuilder.invoke(unCachedEjis)
            putIToACache(thisModelBuilder, ejaClazz, buildEjiToEja, unCachedEjis)
            ejiToEja = merge(ejiToEja as MutableMap<Any?, Any?>, buildEjiToEja as Map<Any?, Any?>)
        }

        val thisEjac = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEja[eji] }
        return parseColl(thisEjac, rd) as EJR
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
            val thisEjiToEjaCacheResp = thisModelBuilder.getGlobalIToACache(ejaClazz, setOf(thisEji))
            val thisEjiToEjaCached = thisEjiToEjaCacheResp.cached as Map<EJP?, EJR>
            if (thisEjiToEjaCached.size == 1) return thisEjiToEjaCached[thisEji]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjiCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEji = iToEjiCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iToEjiCacheResp.unCached as Collection<I>

        if (unCachedIs.isNotEmpty()) {
            val buildIToEji = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEji, unCachedIs)
            iToEji = merge(iToEji as MutableMap<I, EJP?>, buildIToEji)
        }

        val ejic = iToEji.values
        val cacheResp = thisModelBuilder.getGlobalIToACache(ejaClazz, ejic)
        var ejiToEja = cacheResp.cached as Map<EJP?, EJR>
        val unCachedEjis = cacheResp.unCached as Collection<EJP?>

        if (unCachedEjis.isNotEmpty()) {
            val ejaBuilder = getBuilder<EJR, EJP?>(ejaClazz)
            val buildEjiToEja = ejaBuilder.invoke(unCachedEjis)
            putIToACache(thisModelBuilder, ejaClazz, buildEjiToEja, unCachedEjis)
            ejiToEja = merge(ejiToEja as MutableMap<EJP?, EJR>, buildEjiToEja)
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
            val thisEjac = (thisICache[thisI] as Collection<*>).filter { it != null } as List<Any>
            val thisEjacToEjtcCacheResp = thisModelBuilder.getGlobalAToTCache(ejtClazz, thisEjac)
            val thisEjacToEjtcCached = thisEjacToEjtcCacheResp.cached
            if (thisEjacToEjtcCached.size == thisEjac.size) {
                val thisEjtc = thisEjacToEjtcCached.values.filter { it != null } as List<Any>
                return parseColl(thisEjtc, rd) as EJR
            }
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjacCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEjac = iToEjacCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iToEjacCacheResp.unCached as Collection<I>

        if (unCachedIs.isNotEmpty()) {
            val buildIToEjac = mapper.invoke(allI)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEjac, unCachedIs)
            iToEjac = merge(iToEjac as MutableMap<I, EJP?>, buildIToEjac)
        }

        val ejac = flatAndFilterNonNull(iToEjac.values)
        val ejaToEjtCacheResp  = thisModelBuilder.getGlobalAToTCache(ejtClazz, ejac)
        var ejaToEjt: Map<Any?, Any?> = ejaToEjtCacheResp.cached
        val unCachedEjas = ejaToEjtCacheResp.unCached as Collection<Any>

        if (unCachedEjas.isNotEmpty()) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjas
            ejaToEjt = merge(ejaToEjt as MutableMap<Any?, Any?>, ejModelBuilder.currAToT as Map<Any?, Any?>)
        }

        val thisEjtc = (iToEjac[thisI] as Collection<Any>).map { eja -> ejaToEjt[eja] }
        return parseColl(thisEjtc, rd) as EJR
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
            val thisEjaToEjtCacheResp = thisModelBuilder.getGlobalAToTCache(ejtClazz, setOf(thisEja))
            val thisEjaToEjtCached = thisEjaToEjtCacheResp.cached as MutableMap<EJP?, EJR>
            if (thisEjaToEjtCached.size == 1) return thisEjaToEjtCached[thisEja]
        }

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjaCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEja = iToEjaCacheResp.cached as Map<I, EJP?>
        val unCachedIs = iToEjaCacheResp.unCached as Collection<I>

        if (unCachedIs.isNotEmpty()) {
            val buildIToEja = mapper.invoke(unCachedIs)
            putIToEjmCache(thisModelBuilder, mapper, buildIToEja, unCachedIs)
            iToEja = merge(iToEja as MutableMap<I, EJP?>, buildIToEja)
        }

        val ejas = iToEja.values
        val cacheResp = thisModelBuilder.getGlobalAToTCache(ejtClazz, ejas)
        var ejaToEjt = cacheResp.cached as Map<EJP?, EJR>
        val unCachedEjas = cacheResp.unCached as Collection<EJP?>

        if (unCachedEjas.isNotEmpty()) {
            val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)
            Scopes.setModelBuilder(ejModelBuilder)

            ejModelBuilder buildMulti ejtClazz by unCachedEjas
            val buildEjaToEjt = ejModelBuilder.currAToT as Map<EJP?, EJR>
            ejaToEjt = merge(ejaToEjt as MutableMap<EJP?, EJR>, buildEjaToEjt)
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
        if (thisICache.size == 1) return thisICache[thisI]

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjmCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEjm = iToEjmCacheResp.cached as Map<I, EJR?>
        val unCachedIs = iToEjmCacheResp.unCached as Collection<I>
        if (unCachedIs.isEmpty()) return iToEjm[thisI]

        val buildIToEjm = mapper.invoke(unCachedIs)
        putIToEjmCache(thisModelBuilder, mapper, buildIToEjm, unCachedIs)
        iToEjm = merge(iToEjm as MutableMap<I, EJR?>, buildIToEjm as Map<I, EJR?>)

        return iToEjm[thisI]
    }

    // C[I]->M[I,EJM]
    private fun handleIToEjm(
        thisModelBuilder: ModelBuilder,
        thisI: I
    ): EJR? {
        val thisICache = thisModelBuilder
            .getGlobalIToEjmCache(mapper, setOf(thisI)).cached as MutableMap<I, EJR>
        if (thisICache.size == 1) return thisICache[thisI]

        val allI = thisModelBuilder.currAllI as Set<I>
        val iToEjmCacheResp = thisModelBuilder.getGlobalIToEjmCache(mapper, allI)
        var iToEjm = iToEjmCacheResp.cached as Map<I, EJR?>
        val unCachedIs = iToEjmCacheResp.unCached as Collection<I>
        if (unCachedIs.isEmpty()) return iToEjm[thisI]

        val buildIToEjm = mapper.invoke(unCachedIs)
        putIToEjmCache(thisModelBuilder, mapper, buildIToEjm, unCachedIs)
        iToEjm = merge(iToEjm as MutableMap<I, EJR?>, buildIToEjm as Map<I, EJR?>)

        return iToEjm[thisI]
    }

    private fun putIToEjmCache(
        ejModelBuilder: ModelBuilder,
        mapper: (Collection<I>) -> Map<I, EJP?>,
        buildIToEjm: Map<I, EJP?>,
        unCachedIs: Collection<I>
    ) {
        ejModelBuilder.putGlobalIToEjmCache(mapper, buildIToEjm)
        if (buildIToEjm.size == unCachedIs.size) return

        val unFetchedIs = unCachedIs.filter { !buildIToEjm.keys.contains(it) }
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

        val unFetchedIs = unCachedIs.filter { !buildIToA.keys.contains(it) }
        val unFetchedIToA = unFetchedIs.associateWith { null }
        modelBuilder.putGlobalIToACache(AClazz, unFetchedIToA)
    }

    companion object {
        fun <I: Any, EJP: Any, EJR: Any> exJoin(mapper: (Collection<I>) -> Map<I, EJP?>) =
            ExJoinDelegate<I, Any, EJP, EJR>(mapper)
    }
}