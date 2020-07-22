package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.utils.WeakIdentityHashMap
import com.agile4j.model.builder.utils.reverseKV
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */
@Suppress("UNCHECKED_CAST")
class ModelBuilder {


    /**
     * ----------curr build data
     */

    internal val indexToAccompanyMap: MutableMap<Any, Any> = mutableMapOf()
    // target做key + WeakIdentityHashMap，防止内存泄露
    internal val targetToAccompanyMap: MutableMap<Any, Any> = WeakIdentityHashMap()

    private val accompanyToIndexMap: Map<Any, Any> get() = indexToAccompanyMap.reverseKV()
    private val targetToIndexMap: Map<Any, Any> get() {
        val accompanyToIndexMap = this.accompanyToIndexMap
        return this.targetToAccompanyMap.mapValues {
                targetToAccompany -> accompanyToIndexMap[targetToAccompany.value]
            ?: err("accompany ${targetToAccompany.value} no matched index") }
    }
    val indexToTargetMap: Map<Any, Any> get() = targetToIndexMap.reverseKV()
    val accompanyToTargetMap: Map<Any, Any> get() = targetToAccompanyMap.reverseKV()
    val accompanyClazz: KClass<*> get() = indexToAccompanyMap.values.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }
    val targetClazz: KClass<*> get() = targetToAccompanyMap.keys.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("targetToAccompanyMap is empty") }

    /**
     * ----------cache
     */

    /**
     * joinClass => ( joinIndex => joinModel )
     */
    private var joinCacheMap: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()

    /**
     * joinTargetClass => ( joinTarget => joinIndex )
     * joinTarget做key反向存储 + WeakIdentityHashMap，防止内存泄露
     */
    private var joinTargetCacheReverseMap: MutableMap<KClass<*>, WeakIdentityHashMap<Any, Any>> = mutableMapOf()

    /**
     * outJoinPoint => ( accompany => joinModel )
     */
    private var outJoinCacheMap: MutableMap<String, MutableMap<Any, Any>> = mutableMapOf()

    /**
     * outJoinPoint => ( outJoinTarget => accompany )
     * outJoinTarget做key反向存储 + WeakIdentityHashMap，防止内存泄露
     */
    private var outJoinTargetCacheReverseMap: MutableMap<String, WeakIdentityHashMap<Any, Any>> = mutableMapOf()

    fun getJoinCacheMap(joinClazz: KClass<*>) = joinCacheMap.computeIfAbsent(joinClazz) { mutableMapOf() }

    fun <JI, JM> putAllJoinCacheMap(joinClazz: KClass<*>, joinCache: Map<JI, JM>) =
        joinCacheMap.computeIfAbsent(joinClazz) { mutableMapOf() }.putAll(joinCache as Map<Any, Any>)

    fun getJoinTargetCacheMap(joinTargetClazz: KClass<*>) = joinTargetCacheReverseMap
        .computeIfAbsent(joinTargetClazz) { WeakIdentityHashMap() }.reverseKV()

    fun <JI, JT> putAllJoinTargetCacheMap(joinTargetClazz: KClass<*>, joinTargetCache: Map<JI, JT>) =
        joinTargetCacheReverseMap.computeIfAbsent(joinTargetClazz) { WeakIdentityHashMap() }
            .putAll(joinTargetCache.reverseKV() as Map<Any, Any>)

    fun getOutJoinCacheMap(outJoinPoint: String) = outJoinCacheMap.computeIfAbsent(outJoinPoint) { mutableMapOf() }

    fun <A, JM> putAllOutJoinCacheMap(outJoinPoint: String, outJoinCache: Map<A, JM>) =
        outJoinCacheMap.computeIfAbsent(outJoinPoint) { mutableMapOf() }.putAll(outJoinCache as Map<Any, Any>)

    fun getOutJoinTargetCacheMap(outJoinTargetPoint: String) = outJoinTargetCacheReverseMap
        .computeIfAbsent(outJoinTargetPoint) { WeakIdentityHashMap() }.reverseKV()

    fun <A, JT> putAllOutJoinTargetCacheMap(outJoinTargetPoint: String, outJoinTargetCache: Map<A, JT>) =
        outJoinTargetCacheReverseMap.computeIfAbsent(outJoinTargetPoint) { WeakIdentityHashMap() }
            .putAll(outJoinTargetCache.reverseKV() as Map<Any, Any>)

    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便缓存共享
            result.joinTargetCacheReverseMap = from.joinTargetCacheReverseMap
            result.joinCacheMap = from.joinCacheMap
            result.outJoinTargetCacheReverseMap = from.outJoinTargetCacheReverseMap
            result.outJoinCacheMap = from.outJoinCacheMap
            return result
        }
    }
}