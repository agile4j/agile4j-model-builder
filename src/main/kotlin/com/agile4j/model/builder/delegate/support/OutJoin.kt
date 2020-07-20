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
        //val outJoinTargetAccessor = thisRef.buildInModelBuilder.outJoinTargetAccessorMap[outJoinPoint]
        val outJoinTargetAccessor = OutJoinTargetAccessor<Any, Any, Any>(outJoinPoint)
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        return outJoinTargetAccessor!!.get(accompanies)[accompany] as T
        //return access(accompanies, singleton(outJoinTargetAccessor as IAccessor<Any, T>))[accompany] ?: error("")
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildAccompany(thisRef: Any, property: KProperty<*>): T {
        val accompany = thisRef.buildInModelBuilder.targetToAccompanyMap[thisRef]!!
        //val outJoinAccessor = thisRef.buildInModelBuilder.outJoinAccessorMap[outJoinPoint]
        val outJoinAccessor = OutJoinAccessor<Any, Any, Any>(outJoinPoint)
        val accompanies = thisRef.buildInModelBuilder.indexToAccompanyMap.values
        return outJoinAccessor.get(accompanies)[accompany] as T
        //return access(accompanies, singleton(outJoinAccessor as IAccessor<Any, T>))[accompany] ?: error("")
    }
}