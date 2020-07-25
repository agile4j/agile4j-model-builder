package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.BuildContext.builderHolder
import com.agile4j.model.builder.build.BuildContext.getA
import com.agile4j.model.builder.build.BuildContext.getT
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.buildMulti
import com.agile4j.model.builder.by
import java.util.Objects.nonNull
import java.util.stream.Collectors.toSet
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

        val allA = thisModelBuilder.allA.toSet() as Set<A>
        val thisA = thisModelBuilder.tToA[thisT]!! as A

        val pd = IJPDesc(mapper)
        val rd = RDesc(property)

        // A->IJM
        // A->C[IJM]
        if (pd.eq(rd)) {
            // TODO cache? no!
            // TODO print warn
            return mapper.invoke(thisA) as IJR?
        }

        // TODO: all need cache
        
        // A->IJA->IJT: IJP=IJA;IJR=IJT
        if (!pd.isColl() && !rd.isColl() && pd.isA() && rd.isT()) {
            val aToIja = allA.map { a -> a to mapper.invoke(a) }.toMap()
            val ijrClazz = getT(rd.type)!!
            ijModelBuilder buildMulti (ijrClazz) by aToIja.values.toSet()
            val ijaToIjt = ijModelBuilder.aToT as Map<IJP, IJR>
            return ijaToIjt[aToIja[thisA]]
        }

        // A->C[IJA]->C[IJT]: IJP=C[IJA];IPR=C[IJT]
        if (pd.isColl() && rd.isColl() && pd.isA() && rd.isT()) {
            val aToIjac = allA.map { a -> a to mapper.invoke(a) }.toMap()
            val ijas = aToIjac.values.stream()
                .flatMap { ijac -> (ijac as Collection<*>).stream() }
                .filter(::nonNull).map { it!! }.collect(toSet()).toSet()
            val ijtClazz = getT(rd.cType!!)!!
            ijModelBuilder buildMulti (ijtClazz) by ijas
            val thisIjac = aToIjac[thisA] as Collection<Any>
            val thisIjtc = thisIjac.map { ija ->
                ijModelBuilder.aToT[ija] } as Collection<Any>
            // TODO 考虑是否需要
            /*if (Set::class.java.isAssignableFrom(rd.clazz.java)) {
                return thisIjtc.toSet() as IJR
            }
            if (List::class.java.isAssignableFrom(rd.clazz.java)) {
                return thisIjtc.toList() as IJR
            }*/
            return thisIjtc as IJR
        }

        // A->IJI->IJA: IJP=IJI;IJR=IJA
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isA()) {
            val aToIji = allA.map { a -> a to mapper.invoke(a)}.toMap()
            val ijis = aToIji.values.toSet()
            val ijaClazz = getA(rd.type)!!
            val ijaBuilder = builderHolder[ijaClazz]
                    as (Collection<IJP>) -> Map<IJP, IJR>
            val ijiToIja = ijaBuilder.invoke(ijis)
            return ijiToIja[aToIji[thisA]]
        }

        // A->C[IJI]->C[IJA]: IJP=C[IJI];IJR=C[IJA]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isA()) {
            val aToIjic = allA.map { a -> a to mapper.invoke(a) }.toMap()
            val ijis =  aToIjic.values.stream()
                .flatMap { ijic -> (ijic as Collection<*>).stream() }
                .filter(::nonNull).map { it!! }.collect(toSet()).toSet()
            val ijaClazz = getA(rd.cType!!)!!
            val ijaBuilder = builderHolder[ijaClazz]
                    as (Collection<Any>) -> Map<Any, Any>
            val ijiToIja = ijaBuilder.invoke(ijis)
            return (aToIjic[thisA] as Collection<Any>).map { iji -> ijiToIja[iji] } as IJR?
        }

        // A->IJI->IJA->IJT: IJP=IJI;IJR=IJT
        if (!pd.isColl() && !rd.isColl() && pd.isI() && rd.isT()) {
            val aToIji = allA.map { a -> a to mapper.invoke(a) }.toMap()
            val ijis = aToIji.values.toSet()
            val ijtClazz = getT(rd.type)!!
            ijModelBuilder buildMulti ijtClazz by ijis
            val ijiToIjt = ijModelBuilder.iToT as Map<IJP, IJR>
            return ijiToIjt[aToIji[thisA]]
        }

        // A->C[IJI]->C[IJA]->C[IJT]: IJP=C[IJI];IJR=C[IJT]
        if (pd.isColl() && rd.isColl() && pd.isI() && rd.isT()) {
            val aToIjic = allA.map { a -> a to mapper.invoke(a) }.toMap()
            val ijis =  aToIjic.values.stream()
                .flatMap { ijic -> (ijic as Collection<*>).stream() }
                .filter(::nonNull).map { it!! }.collect(toSet()).toSet()
            val ijtClazz = getT(rd.cType!!)!!
            ijModelBuilder buildMulti ijtClazz by ijis
            val ijiToIjt = ijModelBuilder.iToT
            return (aToIjic[thisA] as Collection<Any>).map { iji -> ijiToIjt[iji] } as IJR?
        }

        err("cannot handle")
    }

    /*override fun buildTarget(thisT: Any, property: KProperty<*>): IJR? =
        build(thisT, property, ::joinTargetAccessor)

    override fun buildAccompany(thisT: Any, property: KProperty<*>): IJR? =
        build(thisT, property, ::joinAccessor)*/

    /*private fun build(
        thisT: Any,
        property: KProperty<*>,
        accessor: (KClass<Any>) -> BaseInJoinAccessor<A, IJP, Any>
    ): IJR? {
        val modelBuilder = thisT.buildInModelBuilder
        val joinClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = modelBuilder.targetToAccompanyMap[thisT]!! as A
        val joinIndex = mapper.invoke(accompany)
        return accessor(joinClazz).get(modelBuilder.allA)[accompany]?.get(joinIndex) as IJR?
    }*/

    companion object {
        fun <A: Any, IJP: Any, IJR: Any> inJoin(mapper: (A) -> IJP) =
            InternalJoinDelegate<A, IJP, IJR>(mapper)
    }
}