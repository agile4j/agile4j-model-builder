package com.agile4j.model.builder.build

import com.agile4j.model.builder.exception.ModelBuildException
import com.agile4j.model.builder.utils.reverseKV
import com.github.benmanes.caffeine.cache.Cache
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

    // aClass => ( i => a )
    private var globalIToACache: MutableMap<KClass<*>, Cache<Any?, Any?>> = mutableMapOf()
    // aClass => ( a => i )
    private var globalAToICache: MutableMap<KClass<*>, Cache<Any?, Any?>> = mutableMapOf()
    // tClass => ( t => a )
    private var globalTToACache: MutableMap<KClass<*>, Cache<Any?, Any?>> = mutableMapOf()
    // tClass => ( a => t )
    private var globalAToTCache: MutableMap<KClass<*>, Cache<Any?, Any?>> = mutableMapOf()
    // eJMapper => ( i => ejm )
    private var globalIToEjmCache : MutableMap<(Collection<Any?>) -> Map<Any?, Any?>,
            Cache<Any?, Any?>> = mutableMapOf()

    /**
     * null -> emptyHolder
     */
    private fun encodeEmptyHolder(map: Map<Any?, Any?>) = map
        .mapKeys { e -> e.key?:emptyHolder }
        .mapValues { e -> e.value?:emptyHolder }

    /**
     * emptyHolder -> null
     */
    private fun decodeEmptyHolder(map: Map<Any?, Any?>) = map
        .mapKeys { e -> if (e.key == emptyHolder) null else e.key }
        .mapValues { e -> if (e.value == emptyHolder) null else e.value }

    fun <I, A> putAllIToACache(aClazz: KClass<*>, iToACache: Map<I, A>) {
        this.globalIToACache.computeIfAbsent(aClazz) { Caffeine.newBuilder().build() }
            .putAll(encodeEmptyHolder(iToACache as Map<Any?, Any?>))
        this.globalAToICache.computeIfAbsent(aClazz) { Caffeine.newBuilder().build() }
            .putAll(encodeEmptyHolder(iToACache.reverseKV()))
    }

    fun getIToACache(aClazz: KClass<*>) = decodeEmptyHolder(
        globalIToACache.computeIfAbsent(aClazz) { Caffeine.newBuilder().build() }.asMap())

    fun getAToTCache(tClazz: KClass<*>) = decodeEmptyHolder(
        globalAToTCache.computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }.asMap())

    fun <T, A> putAllTToACache(tClazz: KClass<*>, tToACache: Map<T, A>) {
        this.globalTToACache.computeIfAbsent(tClazz) { Caffeine.newBuilder().weakKeys().build() }
            .putAll(encodeEmptyHolder(tToACache as Map<Any?, Any?>))
        this.globalAToTCache.computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }
            .putAll(encodeEmptyHolder(tToACache.reverseKV()))
    }

    fun getIToTCache(tClazz: KClass<*>): Map<Any?, Any?> {
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val iToA = decodeEmptyHolder(globalIToACache
            .computeIfAbsent(aClazz) { Caffeine.newBuilder().build() }.asMap())
        val aToT = decodeEmptyHolder(globalAToTCache
            .computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }.asMap())
        return iToA.mapValues { i2a -> aToT[i2a.value]}.filter { it.value != null }
    }

    fun <I, EJM> getIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>) =
        decodeEmptyHolder(globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>)
        { Caffeine.newBuilder().build() }.asMap())

    fun <I, EJM> putAllIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>, iToEjmCache: Map<I, EJM>) {
        this.globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>)
        { Caffeine.newBuilder().build() }.putAll(encodeEmptyHolder(iToEjmCache as Map<Any?, Any?>))
    }


    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便缓存共享
            result.globalIToACache = from.globalIToACache
            result.globalAToICache = from.globalAToICache
            result.globalTToACache = from.globalTToACache
            result.globalAToTCache = from.globalAToTCache
            result.globalIToEjmCache = from.globalIToEjmCache
            return result
        }
    }
}