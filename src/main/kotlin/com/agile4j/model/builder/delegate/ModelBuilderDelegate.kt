package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.delegate.map.WeakIdentityHashMap
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
class ModelBuilderDelegate {

    /**
     * eg: movieView -> modelBuilder
     */
    private val mutableMap: WeakIdentityHashMap<Any, ModelBuilder> = buildMap()

    private fun buildMap() : WeakIdentityHashMap<Any, ModelBuilder> {
        return WeakIdentityHashMap()
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder {
        //println("***hashCode:" + identityHashCode(mutableMap) + " size:" + mutableMap.size + " mutableMap:" + mutableMap)
        return mutableMap[thisRef] ?: throw ModelBuildException("$thisRef not init modelBuilder")
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder) {
        /*if (mutableMap.containsKey(thisRef)) {
            throw ModelBuildException("$thisRef already init modelBuilder")
        }*/
        mutableMap[thisRef] = value
    }
}