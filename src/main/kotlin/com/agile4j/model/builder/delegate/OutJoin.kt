package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.buildInModelBuilder
import com.agile4j.utils.access.IAccessor
import com.agile4j.utils.access.access
import java.util.Collections.singleton
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
class OutJoin<T>(private val outJoinPoint: String) : IBuildDelegate<T> {

    @Suppress("UNCHECKED_CAST")
    override fun buildTarget(thisRef: Any, property: KProperty<*>): T {
        val accompany = thisRef.buildInModelBuilder!!.targetToAccompanyMap[thisRef]!!
        val outJoinTargetAccessor = thisRef.buildInModelBuilder!!.outJoinTargetAccessorMap[outJoinPoint]
        val accompanies = thisRef.buildInModelBuilder!!.indexToAccompanyMap.values
        return access(accompanies, singleton(outJoinTargetAccessor as IAccessor<Any, T>))[accompany] ?: error("")
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildAccompany(thisRef: Any, property: KProperty<*>): T {
        val accompany = thisRef.buildInModelBuilder!!.targetToAccompanyMap[thisRef]!!
        val outJoinAccessor = thisRef.buildInModelBuilder!!.outJoinAccessorMap[outJoinPoint]
        val accompanies = thisRef.buildInModelBuilder!!.indexToAccompanyMap.values
        return access(accompanies, singleton(outJoinAccessor as IAccessor<Any, T>))[accompany] ?: error("")
    }
}