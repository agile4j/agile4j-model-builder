package com.agile4j.model.builder.delegate.support

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
        val joinTargetClazz = property.returnType.jvmErasure
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        //val joinTargetAccessor = thisRef.buildInModelBuilder.joinTargetAccessorMap[joinTargetClazz]
        val joinTargetAccessor = JoinTargetAccessor<Any, Any, Any>(joinTargetClazz as KClass<Any>)
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        val joinAccompanyIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }
            .findFirst().map { it.get(accompany) }.orElse(null)


        // TODO fix error
        val temp = joinTargetAccessor!!.get(accompanies)
        val temp1 = temp[accompany] ?: error("123")
        val temp2 = temp1[joinAccompanyIndex] as T
        return temp2
        //return (joinTargetAccessor!!.get(accompanies)[accompany] ?: error("123")) [joinAccompanyIndex] as T
        /*val result =  (access(accompanies, singleton(joinTargetAccessor as
                IAccessor<Any, Map<Any, Any>>))[accompany] ?: error("accompany:$accompany"))[joinAccompanyIndex] as T
        return result*/
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildAccompany(thisRef: Any, property: KProperty<*>): T {
        val joinClazz = property.returnType.jvmErasure
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        //val joinAccessor = thisRef.buildInModelBuilder.joinAccessorMap[joinClazz]
        val joinAccessor = JoinAccessor<Any, Any, Any>(joinClazz)
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        val joinIndex = accompany.javaClass.kotlin.memberProperties.stream()
            .filter { joinFieldName == it.name }
            .findFirst().map { it.get(accompany) }.orElse(null)
        //val iAccessor = joinAccessor as IAccessor<Any, Map<Any, Any>>
        return (joinAccessor.get(accompanies)[accompany] ?: error("456"))[joinIndex] as T
        /*return (access(accompanies, singleton(iAccessor))[accompany]
            ?: throw ModelBuildException("access $accompany err."))[joinIndex] as T*/
    }
}