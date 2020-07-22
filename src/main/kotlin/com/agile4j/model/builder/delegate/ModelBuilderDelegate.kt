package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.utils.WeakIdentityHashMap
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
class ModelBuilderDelegate {

    /**
     * target => modelBuilder
     */
    private val mutableMap: WeakIdentityHashMap<Any, ModelBuilder> = WeakIdentityHashMap()

    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder {
        //println("***hashCode:" + identityHashCode(mutableMap) + " size:" + mutableMap.size + " mutableMap:" + mutableMap)
        return mutableMap[thisRef] ?: err("$thisRef not init modelBuilder")
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder) {
        if (mutableMap.containsKey(thisRef)) err("$thisRef already init modelBuilder")
        mutableMap[thisRef] = value
    }
}