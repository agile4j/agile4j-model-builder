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

    internal val iToA: MutableMap<Any, Any> = mutableMapOf()
    // target做key + WeakIdentityHashMap，防止内存泄露
    internal val tToA: MutableMap<Any, Any> = WeakIdentityHashMap()

    val aToI: Map<Any, Any> get() = iToA.reverseKV()
    private val tToI: Map<Any, Any> get() {
        val aToI = this.aToI
        return this.tToA.mapValues { t2a -> aToI[t2a.value]
            ?: err("accompany ${t2a.value} no matched index") }
    }
    val iToT: Map<Any, Any> get() = tToI.reverseKV()
    val aToT: Map<Any, Any> get() = tToA.reverseKV()
    val aClazz: KClass<*> get() = iToA.values.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }
    val tClazz: KClass<*> get() = tToA.keys.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("targetToAccompanyMap is empty") }
    val allA get() = iToA.values.toSet()
    val allI get() = iToA.keys.toSet()


    /**
     * // TODO : 注意：缓存的是I、A、T之间的关系，以及EJI和EJM的关系
     * aClass => (i => a)
     * tClass => (t => a)
     *
     * EJFunction => (i => m)
     * ----------cache
     */

    /**
     * aClass => ( i => a )
     */
    private var iToACache: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()

    /**
     * tClass => ( t => a )
     */
    private var tToACache: MutableMap<KClass<*>, WeakIdentityHashMap<Any, Any>> = mutableMapOf()

    /**
     * eJMapper => ( i => ejm )
     */
    private var iToEjmCache: MutableMap<String, MutableMap<Any, Any>> = mutableMapOf()

    /**
     * outJoinPoint => ( outJoinTarget => accompany )
     * outJoinTarget做key反向存储 + WeakIdentityHashMap，防止内存泄露
     */
    /*private var outJoinTargetCacheReverseMap: MutableMap<String, WeakIdentityHashMap<Any, Any>> = mutableMapOf()*/

    fun getIToACache(aClazz: KClass<*>) = iToACache
        .computeIfAbsent(aClazz) { mutableMapOf() }

    fun <I, A> putAllIToACache(aClazz: KClass<*>, iToACache: Map<I, A>) = this.iToACache
        .computeIfAbsent(aClazz) { mutableMapOf() }.putAll(iToACache as Map<Any, Any>)

    fun getTToACache(tClazz: KClass<*>) = tToACache
        .computeIfAbsent(tClazz) { WeakIdentityHashMap() }

    fun getAToTCache(tClazz: KClass<*>) = tToACache
        .computeIfAbsent(tClazz) { WeakIdentityHashMap() }.reverseKV()

    fun <T, A> putAllTToACache(tClazz: KClass<*>, tToACache: Map<T, A>) = this.tToACache
        .computeIfAbsent(tClazz) { WeakIdentityHashMap() }.putAll(tToACache as Map<Any, Any>)

    fun getIToTCache(tClazz: KClass<*>): Map<Any, Any> {
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val aToI = getIToACache(aClazz).reverseKV()
        val tToA = tToACache[tClazz]
        return tToA?.mapValues { t2a -> aToI[t2a.value]!! }?.reverseKV() ?: emptyMap()
    }


    fun getOutJoinCacheMap(outJoinPoint: String) = iToEjmCache.computeIfAbsent(outJoinPoint) { mutableMapOf() }

    fun <A, JM> putAllOutJoinCacheMap(outJoinPoint: String, outJoinCache: Map<A, JM>) =
        iToEjmCache.computeIfAbsent(outJoinPoint) { mutableMapOf() }.putAll(outJoinCache as Map<Any, Any>)

    /*fun getOutJoinTargetCacheMap(outJoinTargetPoint: String) = outJoinTargetCacheReverseMap
        .computeIfAbsent(outJoinTargetPoint) { WeakIdentityHashMap() }.reverseKV()

    fun <A, JT> putAllOutJoinTargetCacheMap(outJoinTargetPoint: String, outJoinTargetCache: Map<A, JT>) =
        outJoinTargetCacheReverseMap.computeIfAbsent(outJoinTargetPoint) { WeakIdentityHashMap() }
            .putAll(outJoinTargetCache.reverseKV() as Map<Any, Any>)*/

    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便缓存共享
            result.tToACache = from.tToACache
            result.iToACache = from.iToACache
            //result.outJoinTargetCacheReverseMap = from.outJoinTargetCacheReverseMap
            result.iToEjmCache = from.iToEjmCache
            return result
        }
    }
}