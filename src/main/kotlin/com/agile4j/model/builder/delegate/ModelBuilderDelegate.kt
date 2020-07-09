package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.ModelBuilder
import java.lang.System.identityHashCode
import java.util.*
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
class ModelBuilderDelegate {
    private val mutableMap: MutableMap<Any, MutableMap<String, ModelBuilder>> = buildMap()

    private fun buildMap() : WeakHashMap<Any, MutableMap<String, ModelBuilder>> {
        println("###buildMap")
        return WeakHashMap()
        //return HashMap()
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder {
        println("***hashCode:" + identityHashCode(mutableMap) + " size:" + mutableMap.size + " mutableMap:" + mutableMap)
        if (mutableMap[thisRef]?.get(property.toString()) == null) {
            mutableMap.computeIfAbsent(thisRef) { mutableMapOf() }
            mutableMap[thisRef]!![property.toString()] = ModelBuilder()
        }
        return mutableMap[thisRef]!![property.toString()]!!
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder) {
        mutableMap.computeIfAbsent(thisRef) { mutableMapOf() }
        mutableMap[thisRef]!![property.toString()] = value
    }
}