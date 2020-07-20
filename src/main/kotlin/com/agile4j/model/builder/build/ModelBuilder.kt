package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.delegate.map.WeakIdentityHashMap
import com.agile4j.model.builder.utils.reverseKV
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */
@Suppress("UNCHECKED_CAST")
class ModelBuilder {


    /**
     * ----------该部分字段存储本次构建的targets，及其对应的accompanies和indices
     * 只有indexToAccompanyMap、targetToAccompanyMap是占存储空间的，其他字段都是基于这两者计算而来
     */

    val indexToAccompanyMap: MutableMap<Any, Any> = mutableMapOf()
    // target做key + WeakIdentityHashMap，防止内存泄露
    val targetToAccompanyMap: MutableMap<Any, Any> = WeakIdentityHashMap()

    val targetToIndexMap: Map<Any, Any> get() {
        val accompanyToIndexMap = this.accompanyToIndexMap
        return this.targetToAccompanyMap.mapValues {
                targetToAccompany -> accompanyToIndexMap[targetToAccompany.value]
            ?: throw ModelBuildException("accompany ${targetToAccompany.value} no matched index") }
    }
    val accompanyToIndexMap: Map<Any, Any> get() = indexToAccompanyMap.reverseKV()
    val indexToTargetMap: Map<Any, Any> get() = targetToIndexMap.reverseKV()
    val accompanyClazz: KClass<*> get() = indexToAccompanyMap.values.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }
    val targetClazz: KClass<*> get() = targetToAccompanyMap.keys.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("targetToAccompanyMap is empty") }

    /**
     * ----------该部分字段存储本次构建的中间数据，避免重复build
     */

    /**
     * joinClass -> ( joinIndex -> joinModel )
     */
    private var joinCacheMap: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()

    /**
     * joinTargetClass -> ( joinTarget -> joinIndex )
     * joinTarget做key反向存储 + WeakIdentityHashMap，防止内存泄露
     */
    private var joinTargetCacheReverseMap: MutableMap<KClass<*>, WeakIdentityHashMap<Any, Any>> = mutableMapOf()

    /**
     * outJoinPoint -> ( accompany -> joinModel )
     */
    var outJoinCacheMap: MutableMap<String, MutableMap<Any, Any>> = mutableMapOf()

    /**
     * outJoinPoint -> ( joinTarget -> accompany )
     * joinTarget做key反向存储 + WeakIdentityHashMap，防止内存泄露
     */
    var outJoinTargetCacheMap: MutableMap<String, WeakIdentityHashMap<Any, Any>> = mutableMapOf()

    fun getJoinCacheMap(joinClazz: KClass<*>) = joinCacheMap.computeIfAbsent(joinClazz) { mutableMapOf() }

    fun <JI, JM> putAllJoinCacheMap(joinClazz: KClass<*>, joinCache: Map<JI, JM>) =
        joinCacheMap.computeIfAbsent(joinClazz) { mutableMapOf() }.putAll(joinCache as Map<Any, Any>)

    fun getJoinTargetCacheMap(joinTargetClazz: KClass<*>) = joinTargetCacheReverseMap
        .computeIfAbsent(joinTargetClazz) { WeakIdentityHashMap() }.reverseKV()

    fun <JI, JT> putAllJoinTargetCacheMap(joinTargetClazz: KClass<*>, joinTargetCache: Map<JI, JT>) =
        joinTargetCacheReverseMap.computeIfAbsent(joinTargetClazz) { WeakIdentityHashMap() }
            .putAll(joinTargetCache.reverseKV() as Map<Any, Any>)

    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便缓存共享
            result.joinTargetCacheReverseMap = from.joinTargetCacheReverseMap
            result.joinCacheMap = from.joinCacheMap
            result.outJoinTargetCacheMap = from.outJoinTargetCacheMap
            result.outJoinCacheMap = from.outJoinCacheMap
            return result
        }
    }
}