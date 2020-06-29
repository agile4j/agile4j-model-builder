package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.isBuildTargetClass
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-18
 */
interface ModelBuilderDelegate<T>{
    operator fun getValue(thisRef: Any, property: KProperty<*>): T =
        if (isBuildTargetClass(property)) buildTarget(thisRef, property)
        else buildAccompany(thisRef, property)

    fun buildTarget(thisRef: Any, property: KProperty<*>): T

    fun buildAccompany(thisRef: Any, property: KProperty<*>): T

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        throw UnsupportedOperationException("model builder delegate field not support set")
    }
}