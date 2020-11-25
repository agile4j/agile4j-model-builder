package com.agile4j.model.builder.exception

import com.agile4j.model.builder.delegate.EJPDesc
import com.agile4j.model.builder.delegate.IJPDesc
import com.agile4j.model.builder.delegate.RDesc
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */

class ExJoinExceptionContext<I: Any, A:Any, T:Any, EJP: Any>(
    throwable: Throwable,
    thisTarget: T,
    thisAccompany: A,
    thisIndex: I,
    property: KProperty<*>,
    mapper: (Collection<I>) -> Map<I, EJP?>,
    pruner: () -> Boolean,
    provideTypeDesc: EJPDesc<I, EJP>,
    requireTypeDesc: RDesc
): BaseExceptionContext<I, A, T, (Collection<I>) -> Map<I, EJP?>, EJPDesc<I, EJP>>(
    throwable, JoinMode.ExJoin, thisTarget, thisAccompany, thisIndex,
    property, mapper, pruner, provideTypeDesc, requireTypeDesc)

class InJoinExceptionContext<I: Any, A:Any, T:Any, IJP: Any>(
    throwable: Throwable,
    thisTarget: T,
    thisAccompany: A,
    thisIndex: I,
    property: KProperty<*>,
    mapper: (A) -> IJP?,
    pruner: () -> Boolean,
    provideTypeDesc: IJPDesc<A, IJP>,
    requireTypeDesc: RDesc
): BaseExceptionContext<I, A, T, (A) -> IJP?, IJPDesc<A, IJP>>(
    throwable, JoinMode.InJoin, thisTarget, thisAccompany, thisIndex,
    property, mapper, pruner, provideTypeDesc, requireTypeDesc)

enum class JoinMode{
    ExJoin, InJoin
}

open class BaseExceptionContext<I: Any, A:Any, T:Any, M, PD>(
    val throwable: Throwable,
    val joinMode: JoinMode,
    val thisTarget: T,
    val thisAccompany: A,
    val thisIndex: I,
    val property: KProperty<*>,
    val mapper: M,
    val pruner: () -> Boolean,
    val provideTypeDesc: PD,
    val requireTypeDesc: RDesc
) {
    override fun toString(): String {
        return "BaseExceptionContext(throwable=$throwable, thisTarget=$thisTarget," +
                "thisAccompany=$thisAccompany, thisIndex=$thisIndex, property=$property," +
                "mapper=$mapper, pruner=$pruner, provideTypeDesc=$provideTypeDesc," +
                "requireTypeDesc=$requireTypeDesc)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseExceptionContext<*, *, *, *, *>

        if (throwable != other.throwable) return false
        if (thisTarget != other.thisTarget) return false
        if (thisAccompany != other.thisAccompany) return false
        if (thisIndex != other.thisIndex) return false
        if (property != other.property) return false
        if (mapper != other.mapper) return false
        if (pruner != other.pruner) return false
        if (provideTypeDesc != other.provideTypeDesc) return false
        if (requireTypeDesc != other.requireTypeDesc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = throwable.hashCode()
        result = 31 * result + thisTarget.hashCode()
        result = 31 * result + thisAccompany.hashCode()
        result = 31 * result + thisIndex.hashCode()
        result = 31 * result + property.hashCode()
        result = 31 * result + (mapper?.hashCode() ?: 0)
        result = 31 * result + pruner.hashCode()
        result = 31 * result + (provideTypeDesc?.hashCode() ?: 0)
        result = 31 * result + requireTypeDesc.hashCode()
        return result
    }

}

fun <I: Any, A:Any, T:Any, EJP: Any> contextOf(
    t: Throwable,
    thisT: T,
    thisA: A,
    thisI: I,
    property: KProperty<*>,
    mapper: (Collection<I>) -> Map<I, EJP?>,
    pruner: () -> Boolean,
    pd: EJPDesc<I, EJP>,
    rd: RDesc):  ExJoinExceptionContext<I, A, T, EJP> =
    ExJoinExceptionContext(t, thisT, thisA, thisI, property, mapper, pruner, pd, rd)

fun <I: Any, A:Any, T:Any, IJP: Any> contextOf(
    t: Throwable,
    thisT: T,
    thisA: A,
    thisI: I,
    property: KProperty<*>,
    mapper: (A) -> IJP?,
    pruner: () -> Boolean,
    pd: IJPDesc<A, IJP>,
    rd: RDesc):  InJoinExceptionContext<I, A, T, IJP> =
    InJoinExceptionContext(t, thisT, thisA, thisI, property, mapper, pruner, pd, rd)