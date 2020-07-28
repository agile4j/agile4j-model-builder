package com.agile4j.model.builder.utils

import com.agile4j.model.builder.ModelBuildException.Companion.err
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

/**
 * @author liurenpeng
 * Created on 2020-07-20
 */

fun <K, V> Map<K, V>.reverseKV(): Map<V, K> = this.map { (k, v) -> v to k }.toMap()

internal val <P, R> ((P) -> R).nonNullReturnKClazz: KClass<*> get() = this.returnKClazz
    ?: err("unKnown return kClass of function:$this")

internal val <P, R> ((P) -> R).returnKClazz: KClass<*>? get() = this.returnKType?.jvmErasure

internal val <P, R> ((P) -> R).returnKType: KType? get() =
    if (this is KCallable<*>) this.returnType else this.reflect()?.returnType