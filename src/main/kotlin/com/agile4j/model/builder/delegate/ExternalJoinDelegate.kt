package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getA
import com.agile4j.model.builder.build.BuildContext.getT
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import com.agile4j.utils.util.CollectionUtil
import java.util.*
import java.util.stream.Collectors
import kotlin.reflect.KProperty

/**
 * abbreviations:
 * A        accompany
 * EJP      exJoinProvide
 * EJR      exJoinRequire
 * @author liurenpeng
 * Created on 2020-06-18
 */
@Suppress("UNCHECKED_CAST")
class ExternalJoinDelegate<I: Any, A:Any, EJP: Any, EJR: Any>(
    private val mapper: (Collection<I>) -> Map<I, EJP>) /*: JoinDelegate<EJR>*/ {

    operator fun getValue(thisT: Any, property: KProperty<*>): EJR? {
        val thisModelBuilder = thisT.buildInModelBuilder
        val ejModelBuilder = ModelBuilder.copyBy(thisModelBuilder)

        //val allA = thisModelBuilder.allA as Set<A>
        val allI = thisModelBuilder.allI as Set<I>
        val thisA = thisModelBuilder.tToA[thisT]!! as A
        val thisI = thisModelBuilder.aToI[thisA] as I

        val pd = EJPDesc(mapper)
        val rd = RDesc(property)
        /*println("${pd.type} -- ${pd.isColl()} -- ${pd.cType}")
        println("${rd.type} -- ${rd.isColl()} -- ${rd.cType}")*/

        //val kMapper = mapper as (Collection<Any>) -> Map<Any, Any>

        // C[I]->M[I,EJM]
        // C[I]->M[I,C[EJM]]
        if (pd.eq(rd)) {
            // TODO cache? no!
            // TODO print warn
            val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJR>)
                .filterKeys { allI.contains(it) }
            val unCachedIs = allI.filter { !cached.keys.contains(it) }
            if (CollectionUtil.isEmpty(unCachedIs)) return cached[thisI]

            val iToEjm = mapper.invoke(unCachedIs)
            ejModelBuilder.putAllIToEjmCache(mapper, iToEjm)
            return (iToEjm + cached)[thisI] as EJR?
        }

        // C[I]->M[I,EJA]->M[I,EJT]: EJP=EJA;EJR=EJT
        if (!pd.isColl() && !rd.isColl() && pd.isA() && rd.isT()) {
            val cached = (ejModelBuilder.getIToEjmCache(mapper) as Map<I, EJP>)
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

            val cachedEjaToEjt = (ejModelBuilder.getAToTCache(ejtClazz) as Map<EJP, EJR>)
                .filterKeys { ejas.contains(it) }
            val unCachedEjas = ejas.filter { !cachedEjaToEjt.keys.contains(it) }

            val ejaToEjt = cachedEjaToEjt.toMutableMap()
            if (CollectionUtil.isNotEmpty(unCachedEjas)) {
                ejModelBuilder buildMulti ejtClazz by unCachedEjas
                val buildEjaToEjt = ejModelBuilder.aToT as Map<EJP, EJR>
                ejaToEjt += buildEjaToEjt
            }

            return ejaToEjt[iToEja[thisI]]
        }

        // C[I]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJA];EJR=C[EJT]
        if (pd.isColl() && rd.isColl() && pd.isA() && rd.isT()) {
            val iToEjac = mapper.invoke(allI)
            val ejas = iToEjac.values.stream()
                .flatMap { ejac -> (ejac as Collection<*>).stream() }
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet()).toSet()
            
            val ejtClazz = getT(rd.cType!!)!!
            ejModelBuilder buildMulti ejtClazz by ejas
            val thisEjac = iToEjac[thisI] as Collection<Any>
            val thisEjtc = thisEjac.map { eja ->
                ejModelBuilder.aToT[eja] } as Collection<Any>
            if (rd.isSet()) {
                return thisEjtc.toSet() as EJR
            }
            if (rd.isList()) {
                return thisEjtc.toList() as EJR
            }
            return thisEjtc as EJR
        }

        // C[I]->M[I,EJI]->M[I,EJA]: EJP=EJI;EJR=EJA
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isA()) {
            val iToEji = mapper.invoke(allI)
            val ejis = iToEji.values.toSet()
            val ejaClazz = getA(rd.type)!!
            val ejaBuilder = builderHolder[ejaClazz]
                    as (Collection<EJP>) -> Map<EJP, EJR>
            val ejiToEja = ejaBuilder.invoke(ejis)
            return ejiToEja[iToEji[thisI]]
        }

        // C[I]->M[I,C[EJI]]->M[I,C[EJA]]: EJP=C[EJI];EJR=C[EJA]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isA()) {
            val iToEjic = mapper.invoke(allI)
            val ejis =  iToEjic.values.stream()
                .flatMap { ejic -> (ejic as Collection<*>).stream() }
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet()).toSet()
            val ejaClazz = getA(rd.cType!!)!!
            val ejaBuilder = builderHolder[ejaClazz]
                    as (Collection<Any>) -> Map<Any, Any>
            val ejiToEja = ejaBuilder.invoke(ejis)
            val thisEjac = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEja[eji] }
            if (rd.isSet()) {
                return thisEjac.toSet() as EJR
            }
            if (rd.isList()) {
                return thisEjac.toList() as EJR
            }
            return thisEjac as EJR
        }

        // C[I]->M[I,EJI]->M[I,EJA]->M[I,EJT]: EJP=EJI;EJR=EJT
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isT()) {
            val iToEji = mapper.invoke(allI)
            val ejis = iToEji.values.toSet()
            val ejtClazz = getT(rd.type)!!
            ejModelBuilder buildMulti ejtClazz by ejis
            val ejiToEjt = ejModelBuilder.iToT as Map<EJP, EJR>
            return ejiToEjt[iToEji[thisI]]
        }

        // C[I]->M[I,C[EJI]]->M[I,C[EJA]]->M[I,C[EJT]]: EJP=C[EJI];EJR=C[EJT]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isT()) {
            val iToEjic = mapper.invoke(allI)
            val ejis =  iToEjic.values.stream()
                .flatMap { ejic -> (ejic as Collection<*>).stream() }
                .filter(Objects::nonNull).map { it!! }.collect(Collectors.toSet()).toSet()
            val ejtClazz = getT(rd.cType!!)!!
            ejModelBuilder buildMulti ejtClazz by ejis
            val ejiToEjt = ejModelBuilder.iToT
            val thisEjtc = (iToEjic[thisI] as Collection<Any>).map { eji -> ejiToEjt[eji] }
            if (rd.isSet()) {
                return thisEjtc.toSet() as EJR
            }
            if (rd.isList()) {
                return thisEjtc.toList() as EJR
            }
            return thisEjtc as EJR
        }

        err("cannot handle")
    }

    companion object {
        fun <I: Any, EJP: Any, EJR: Any> exJoin(mapper: (Collection<I>) -> Map<I, EJP>) =
            ExternalJoinDelegate<I, Any, EJP, EJR>(mapper)
    }
}