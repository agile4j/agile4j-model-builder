package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.utils.WeakIdentityHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
class ModelBuilderDelegate {

    /**
     * target => modelBuilder
     */
    private val weakMap: WeakIdentityHashMap<Any, ModelBuilder> = WeakIdentityHashMap()

    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder {
        weakMapSize.set(weakMap.size)
        return weakMap[thisRef] ?: err("$thisRef not init modelBuilder")
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder) {
        if (weakMap.containsKey(thisRef)) err("$thisRef already init modelBuilder")
        weakMap[thisRef] = value
        weakMapSize.set(weakMap.size)
    }

    companion object {
        /**
         * 提供一个观察是否内存泄露的接口
         */
        val weakMapSize = AtomicInteger(0)
    }
}