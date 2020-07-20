package com.agile4j.model.builder.delegate.support

import com.agile4j.model.builder.accessor.OutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor
import com.agile4j.model.builder.build.buildInModelBuilder
import com.agile4j.model.builder.delegate.ITargetDelegate
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class OutJoin<T>(private val outJoinPoint: String) : ITargetDelegate<T> {

    @Suppress("UNCHECKED_CAST")
    override fun buildTarget(thisRef: Any, property: KProperty<*>): T {
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        return  OutJoinTargetAccessor<Any, Any, Any>(outJoinPoint).get(accompanies)[accompany] as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildAccompany(thisRef: Any, property: KProperty<*>): T {
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        return OutJoinAccessor<Any, Any, Any>(outJoinPoint).get(accompanies)[accompany] as T
    }
}