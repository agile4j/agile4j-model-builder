package com.agile4j.model.builder.exception

import com.agile4j.model.builder.delegate.EJPDesc
import com.agile4j.model.builder.delegate.IJPDesc
import com.agile4j.model.builder.delegate.RDesc
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-11-22
 */
data class ExJoinExceptionDTO<I: Any, A:Any, T:Any, EJP: Any>(
    val throwable: Throwable,
    val thisTarget: T,
    val thisAccompany: A,
    val thisIndex: I,
    val property: KProperty<*>,
    val mapper: (Collection<I>) -> Map<I, EJP?>,
    val pruner: () -> Boolean,
    val exJoinProvideTypeDesc: EJPDesc<I, EJP>,
    val requireTypeDesc: RDesc
)

data class InJoinExceptionDTO<I: Any, A:Any, T:Any, IJP: Any>(
    val throwable: Throwable,
    val thisTarget: T,
    val thisAccompany: A,
    val thisIndex: I,
    val property: KProperty<*>,
    val mapper: (A) -> IJP?,
    val pruner: () -> Boolean,
    val inJoinProvideTypeDesc: IJPDesc<A, IJP>,
    val requireTypeDesc: RDesc
)