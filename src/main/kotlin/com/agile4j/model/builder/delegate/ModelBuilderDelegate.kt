package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuilder
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
class ModelBuilderDelegate {
    private val mutableMap: MutableMap<Any, MutableMap<String, ModelBuilder?>> = mutableMapOf()
    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder? {
        return mutableMap[thisRef]?.get(property.toString())
    }
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder?) {
        mutableMap.computeIfAbsent(thisRef) { mutableMapOf() }
        mutableMap[thisRef]!![property.toString()] = value
    }
}