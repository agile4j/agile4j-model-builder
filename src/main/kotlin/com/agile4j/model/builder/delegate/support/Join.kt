package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.buildInModelBuilder
import com.agile4j.model.builder.delegate.ITargetDelegate
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.access.access
import java.util.Collections.singleton
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class Join<T>(private val joinFieldName: String) : ITargetDelegate<T> {

    @Suppress("UNCHECKED_CAST")
    override fun buildTarget(thisRef: Any, property: KProperty<*>): T {
        val joinTargetClazz = property.returnType.jvmErasure
        val accompany = thisRef.buildInModelBuilder!!.targetToAccompanyMap[thisRef]!!
        val joinTargetAccessor = thisRef.buildInModelBuilder!!.joinTargetAccessorMap[joinTargetClazz]
        val accompanies = thisRef.buildInModelBuilder!!.indexToAccompanyMap.values
        val joinAccompanyIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }
            .findFirst().map { it.get(accompany) }.orElse(null)
        return (access(accompanies, singleton(joinTargetAccessor as
                IAccessor<Any, Map<Any, Any>>))[accompany] ?: error(""))[joinAccompanyIndex] as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildAccompany(thisRef: Any, property: KProperty<*>): T {
        val joinClazz = property.returnType.jvmErasure
        val accompany = thisRef.buildInModelBuilder!!.targetToAccompanyMap[thisRef]!!
        val joinAccessor = thisRef.buildInModelBuilder!!.joinAccessorMap[joinClazz]
        val accompanies = thisRef.buildInModelBuilder!!.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }
            .findFirst().map { it.get(accompany) }.orElse(null)
        return (access(accompanies, singleton(joinAccessor as
                IAccessor<Any, Map<Any, Any>>))[accompany] ?: error(""))[joinIndex] as T
    }
}