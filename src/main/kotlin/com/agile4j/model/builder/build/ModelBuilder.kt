package com.agile4j.model.builder.build

import com.agile4j.model.builder.exception.ModelBuildException
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.utils.reverseKV
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.*
import kotlin.reflect.KClass

/**
 * abbreviations:
 * T        target
 * A        accompany
 * I        index
 * EJ       exJoin
 * EJM      exJoinModel
 * @author liurenpeng
 * Created on 2020-07-09
 */
@Suppress("UNCHECKED_CAST")
class ModelBuilder {

    /**
     * ----------curr build data
     */

    lateinit var allA: Set<Any?>
    lateinit var allI: Set<Any?>
    lateinit var aClazz: KClass<*>
    lateinit var tClazz: KClass<*>

    private val tToACache = Caffeine.newBuilder().weakKeys().build<Any?, Any?>()
    private val aToICache = Caffeine.newBuilder().build<Any?, Any?>()
    private val aToTCache = Caffeine.newBuilder().weakValues().build<Any?, Any?>()
    private val iToTCache = Caffeine.newBuilder().weakValues().build<Any?, Any?>()

    val aToT: Map<Any?, Any?> get() = aToTCache.asMap()
    val iToT: Map<Any?, Any?> get() = iToTCache.asMap()

    fun putIAT(i2a: Map<*, *>, t2a: Map<*, *>) {
        allA = HashSet(i2a.values)
        allI = HashSet(i2a.keys)
        aClazz = i2a.values.stream().filter{ it != null }.map { it as Any }.findAny()
            .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }
        tClazz = t2a.keys.stream().filter{ it != null }.map { it as Any }.findAny()
            .map { it::class }.orElseThrow { ModelBuildException("targetToAccompanyMap is empty") }

        tToACache.putAll(t2a)
        i2a.forEach { (i, a) -> aToICache.put(a!!, i!!) }
        t2a.forEach { (t, a) -> aToTCache.put(a!!, t!!) }
        i2a.forEach { (i, a) -> iToTCache.put(i!!, aToTCache.get(a!!) { null }!!) }
    }

    fun getAByT(t: Any?): Any? = tToACache.get(t!!){ null }
    fun getIByA(a: Any?): Any? = aToICache.get(a!!){ null }

    /**
     * ----------cache
     */

    private val emptyHolder = Any()

    // aClass => ( i => a )
    private var globalIToACache: MutableMap<KClass<*>, MutableMap<Any?, Any?>> = mutableMapOf()
    // tClass => ( t => a )
    private var globalTToACache: MutableMap<KClass<*>, WeakHashMap<Any?, Any?>> = mutableMapOf()
    // eJMapper => ( i => ejm )
    private var globalIToEjmCache : MutableMap<(Collection<Any?>) -> Map<Any?, Any?>, MutableMap<Any?, Any?>> = mutableMapOf()

    fun <I, A> putAllIToACache(aClazz: KClass<*>, iToACache: Map<I, A>) {
        this.globalIToACache.computeIfAbsent(aClazz) { mutableMapOf() }.putAll(iToACache as Map<Any?, Any?>)
    }

    fun getIToACache(aClazz: KClass<*>) = globalIToACache.computeIfAbsent(aClazz) { mutableMapOf() }

    fun getAToTCache(tClazz: KClass<*>) = globalTToACache
        .computeIfAbsent(tClazz) { WeakHashMap() }.reverseKV()

    fun <T, A> putAllTToACache(tClazz: KClass<*>, tToACache: Map<T, A>) = this.globalTToACache
        .computeIfAbsent(tClazz) { WeakHashMap() }.putAll(tToACache as Map<Any?, Any?>)

    fun getIToTCache(tClazz: KClass<*>): Map<Any?, Any?> {
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val aToI = getIToACache(aClazz).reverseKV()
        val tToA = globalTToACache[tClazz]
        return (tToA?.mapValues { t2a -> aToI[t2a.value]
            ?: err("accompany ${t2a.value} no matched index") }?.reverseKV() ?: emptyMap()) as Map<Any?, Any?>
    }

    fun <I, EJM> getIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>) =
        globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>) { mutableMapOf() }

    fun <I, EJM> putAllIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>, iToEjmCache: Map<I, EJM>) =
        this.globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>) { mutableMapOf() }
            .putAll(iToEjmCache as Map<Any?, Any?>)

    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便缓存共享
            result.globalTToACache = from.globalTToACache
            result.globalIToACache = from.globalIToACache
            result.globalIToEjmCache = from.globalIToEjmCache
            return result
        }
    }
}