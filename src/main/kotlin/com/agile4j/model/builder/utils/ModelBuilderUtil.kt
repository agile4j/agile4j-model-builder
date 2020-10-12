package com.agile4j.model.builder.utils

import com.agile4j.model.builder.delegate.Descriptor
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import java.lang.reflect.Type
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

/**
 * @author liurenpeng
 * Created on 2020-07-20
 */

internal val <P, R> ((P) -> R).nonNullReturnKClazz: KClass<*> get() = this.returnKClazz
    ?: err("unKnown return kClass of function:$this")

internal val <P, R> ((P) -> R).returnKClazz: KClass<*>? get() = this.returnKType?.jvmErasure

internal val <P, R> ((P) -> R).returnKType: KType? get() =
    if (this is KCallable<*>) this.returnType else this.reflect()?.returnType

fun unifyTypeName(type: Type?) = if (type == null) "" else unifyTypeName(type.typeName)

/**
 * 统一java原子类型的typeName
 */
fun unifyTypeName(typeName: String) = when (typeName) {
    "java.lang.Long" -> "long"
    "java.lang.Integer" -> "int"
    "java.lang.Boolean" -> "boolean"
    "java.lang.Float" -> "float"
    "java.long.Double" -> "double"
    "java.long.Byte" -> "byte"
    "java.long.Short" -> "short"
    "java.long.Character" -> "char"
    else -> typeName
}

fun flatAndFilterNonNull(coll: Collection<Any?>): Collection<Any> {
    val result = mutableSetOf<Any>()
    coll.forEach {
        if (it != null) {
            val c = it as Collection<*>
            c.forEach{ e ->
                if (e != null) {
                    result.add(e)
                }
            }
        }
    }
    return result
}

fun <E> parseColl(list: List<E>, desc: Descriptor): Collection<E> =
    if (desc.isSet) list.toSet() else list

fun <K, V> merge(map1: MutableMap<K, V>, map2: Map<K, V>): Map<K, V> =
    if (map1.isEmpty()) map2 else {map1.putAll(map2); map1}

fun getConstructor(tClazz: KClass<*>, aClazz: KClass<*>?): KFunction<*>? {
    for (function in tClazz.constructors) {
        if (function.parameters.size == 1
            && function.parameters[0].type == aClazz?.createType()) return function
    }
    return null
}