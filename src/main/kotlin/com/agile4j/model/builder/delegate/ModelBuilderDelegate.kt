package com.agile4j.model.builder.delegate

import com.agile4j.model.builder.build.ModelBuilder
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty

/**
 * @author liurenpeng
 * Created on 2020-06-30
 */
internal class ModelBuilderDelegate {

    /**
     * target => modelBuilder
     * 该map需要具体4个特性：
     * 1. 线程安全
     * 2. 高并发下仍然性能良好
     * 3. key为弱引用，不影响垃圾回收
     * 4. 通过[System.identityHashCode]而非[Objects.hashCode]计算key的hash值
     */
    private val map = Caffeine.newBuilder().weakKeys().build<Any, ModelBuilder>()

    operator fun getValue(thisRef: Any, property: KProperty<*>): ModelBuilder {
        weakMapSize.set(map.asMap().size)
        return map.getIfPresent(thisRef) ?: err("$thisRef not init modelBuilder")
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: ModelBuilder) {
        if (map.getIfPresent(thisRef) != null) err("$thisRef already init modelBuilder")
        map.put(thisRef, value)
        weakMapSize.set(map.asMap().size)
    }

    companion object {
        /**
         * 提供一个观察是否内存泄露的接口
         */
        val weakMapSize = AtomicInteger(0)
    }
}