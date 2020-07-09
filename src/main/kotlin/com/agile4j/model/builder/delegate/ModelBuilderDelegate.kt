package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.build.ModelBuilder
import java.lang.System.identityHashCode
import java.util.*
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
class ModelBuilderDelegate {

    /**
     * eg: movieView -> modelBuilder
     */
    private val mutableMap: MutableMap<Any, ModelBuilder> = buildMap()

    private fun buildMap() : WeakHashMap<Any, ModelBuilder> {
        return WeakHashMap()
    }

    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder {
        println("***hashCode:" + identityHashCode(mutableMap) + " size:" + mutableMap.size + " mutableMap:" + mutableMap)
        return mutableMap[thisRef] ?: throw ModelBuildException("$thisRef not init modelBuilder")
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder) {
        // TODO 先解决批量如何实现的问题，再回来看这里
        /*if (mutableMap.containsKey(thisRef)) {
            throw ModelBuildException("$thisRef already init modelBuilder")
        }*/
        mutableMap[thisRef] = value
    }
}