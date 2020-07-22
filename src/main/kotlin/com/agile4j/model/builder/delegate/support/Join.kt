package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.ModelBuildException.Companion.err
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
@Suppress("UNCHECKED_CAST")
class Join<T>(private val joinFieldName: String) : ITargetDelegate<T> {

    override fun buildTarget(outerTarget: Any, property: KProperty<*>): T {
        val modelBuilder = outerTarget.buildInModelBuilder
        val joinTargetClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = modelBuilder.targetToAccompanyMap[outerTarget]!!
        val accompanies = modelBuilder.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }.findFirst().map { it.get(accompany) }
            .orElseThrow { ModelBuildException("not found joinIndex. " +
                    "outerTarget:$outerTarget accompany:$accompany joinFieldName:$joinFieldName")}

        val accessor = JoinTargetAccessor<Any, Any, Any>(joinTargetClazz)
        val accompanyMap = accessor.get(accompanies)
        val joinMap = accompanyMap[accompany]
            ?: err("not found accompany:$accompany")
        return joinMap[joinIndex] as T
    }

    override fun buildAccompany(outerTarget: Any, property: KProperty<*>): T {
        val modelBuilder = outerTarget.buildInModelBuilder
        val joinAccompanyClazz = property.returnType.jvmErasure as KClass<Any>
        val accompany = modelBuilder.targetToAccompanyMap[outerTarget]!!
        val accompanies = modelBuilder.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }.findFirst().map { it.get(accompany) }
            .orElseThrow { ModelBuildException("not found joinIndex. " +
                    "outerTarget:$outerTarget accompany:$accompany joinFieldName:$joinFieldName")}

        val accessor = JoinAccessor<Any, Any, Any>(joinAccompanyClazz)
        val accompanyMap =  accessor.get(accompanies)
        val joinMap = accompanyMap[accompany]
            ?: err("not found accompany:$accompany")
        return joinMap[joinIndex] as T
    }
}