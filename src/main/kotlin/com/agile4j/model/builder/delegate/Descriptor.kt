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

open class Descriptor(val type: Type, val cType: Type?) {
    val isColl = isColl(type)
    val isSet = isSet(type)
    val isList = isList(type)

    val isA = if (isColl) isA(cType) else isA(type)
    val isI = if (isColl) isI(cType) else isI(type)
    val isT = if (isColl) isT(cType) else isT(type)

    fun eq(desc: Descriptor): Boolean {
        if (this === desc) return true
        return (isColl && desc.isColl && unifyTypeName(cType) == unifyTypeName(desc.cType))
                || (!isColl && !desc.isColl && unifyTypeName(type) == unifyTypeName(desc.type))
    }
}

/**
 * internal join provide descriptor
 */
class IJPDesc<A: Any, IJP: Any>(mapper: (A) -> IJP?): Descriptor(
    mapper.nonNullReturnKClazz.java,
    if (!isColl(mapper.nonNullReturnKClazz.java)) null else
        (mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(0)
    )

/**
 * external join provide descriptor
 */
class EJPDesc<I: Any, EJP: Any>(mapper: (Collection<I>) -> Map<I, EJP?>): Descriptor(
    (mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(1)!!,
    if (!isColl((mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(1)!!)) null else
        ((mapper.returnKType?.javaType as? ParameterizedType)?.actualTypeArguments?.get(1)!! as? ParameterizedType) ?.actualTypeArguments?.get(0)
)


/**
 * require descriptor
 */
class RDesc(property: KProperty<*>): Descriptor(
    property.returnType.jvmErasure.java,
    if (!isColl(property.returnType.jvmErasure.java)) null else
        (property.returnType.javaType as? ParameterizedType)?.actualTypeArguments?.get(0)
)

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