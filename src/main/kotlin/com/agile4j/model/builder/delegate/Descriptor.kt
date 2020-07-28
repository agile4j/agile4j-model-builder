package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.BuildContext.isA
import com.agile4j.model.builder.build.BuildContext.isI
import com.agile4j.model.builder.build.BuildContext.isT
import com.agile4j.model.builder.utils.nonNullReturnKClazz
import com.agile4j.model.builder.utils.returnKType
import com.agile4j.model.builder.utils.unifyTypeName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

/**
 * 用于描述JP(mapper)和JR(property),以便区分场景
 * @author liurenpeng
 * Created on 2020-07-26
 */

interface Descriptor {
    val type: Type
    val cType: Type? /** 集合泛型type. [isColl]值为true时，才非null */

    fun isColl(): Boolean = isColl(type)
    fun isSet(): Boolean = isSet(type)
    fun isList(): Boolean = isList(type)

    fun isA(): Boolean = if (isColl()) isA(cType) else isA(type)
    fun isI(): Boolean = if (isColl()) isI(cType) else isI(type)
    fun isT(): Boolean = if (isColl()) isT(cType) else isT(type)

    fun eq(desc: Descriptor): Boolean {
        if (this === desc) return true
        return (isColl() && desc.isColl() && unifyTypeName(cType) == unifyTypeName(desc.cType))
                || (!isColl() && !desc.isColl() && unifyTypeName(type) == unifyTypeName(desc.type))
    }
}

/**
 * internal join provide descriptor
 */
class IJPDesc<A: Any, IJP: Any>(private val mapper: (A) -> IJP): Descriptor {
    override val type: Type
        get() = mapper.nonNullReturnKClazz.java
    override val cType: Type?
        get() = if (!isColl()) null else
            (mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(0)
}

/**
 * external join provide descriptor
 */
class EJPDesc<I: Any, EJP: Any>(private val mapper: (Collection<I>) -> Map<I, EJP>): Descriptor {
    override val type: Type
        get() = (mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(1)!!
    override val cType: Type?
        get() = if (!isColl()) null else
            (type as? ParameterizedType) ?.actualTypeArguments?.get(0)
}

/**
 * require descriptor
 */
class RDesc(private val property: KProperty<*>): Descriptor {
    override val type: Type
        get() = property.returnType.jvmErasure.java
    override val cType: Type?
        get() = if (!isColl()) null else
            (property.returnType.javaType as? ParameterizedType)?.actualTypeArguments?.get(0)
}

/**
 * 仅识别[Collection]、[Set]、[List]，如果是其他子类无法识别，请面向接口编程
 */
fun isColl(type: Type?): Boolean {
    if (type?.typeName == null) return false
    return type.typeName.startsWith("java.util.Collection")
            || type.typeName.startsWith("java.util.Set")
            || type.typeName.startsWith("java.util.List")
}

fun isSet(type: Type?): Boolean {
    if (type?.typeName == null) return false
    return type.typeName.startsWith("java.util.Set")
}

fun isList(type: Type?): Boolean {
    if (type?.typeName == null) return false
    return type.typeName.startsWith("java.util.List")
}