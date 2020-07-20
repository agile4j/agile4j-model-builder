package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.accessor.JoinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ITargetDelegate
import kotlin.reflect.KClass
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
        val joinTargetClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }.findFirst().map { it.get(accompany) }
            .orElseThrow { ModelBuildException("not found joinIndex. " +
                    "thisRef:$thisRef accompany:$accompany joinFieldName:$joinFieldName")}

        val accessor = JoinTargetAccessor<Any, Any, Any>(joinTargetClazz)
        val accompanyMap = accessor.get(accompanies)
        val joinMap = accompanyMap[accompany]
            ?: throw ModelBuildException("not found accompany:$accompany")
        return joinMap[joinIndex] as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildAccompany(thisRef: Any, property: KProperty<*>): T {
        val joinAccompanyClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }.findFirst().map { it.get(accompany) }
            .orElseThrow { ModelBuildException("not found joinIndex. " +
                    "thisRef:$thisRef accompany:$accompany joinFieldName:$joinFieldName")}

        val accessor = JoinAccessor<Any, Any, Any>(joinAccompanyClazz)
        val accompanyMap =  accessor.get(accompanies)
        val joinMap = accompanyMap[accompany]
            ?: throw ModelBuildException("not found accompany:$accompany")
        return joinMap[joinIndex] as T
    }
}