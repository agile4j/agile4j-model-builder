package com.agile4j.model.builder.utils

import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import java.lang.reflect.Type
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
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