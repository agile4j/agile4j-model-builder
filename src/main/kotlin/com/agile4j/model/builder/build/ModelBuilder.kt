package com.agile4j.model.builder.build

import com.agile4j.model.builder.exception.ModelBuildException
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
    private var globalIToACache: MutableMap<KClass<*>, MutableMap<Any?, Any?>> = mutableMapOf()
    // aClass => ( a => i )
    private var globalAToICache: MutableMap<KClass<*>, MutableMap<Any?, Any?>> = mutableMapOf()
    // tClass => ( a => t )
    private var globalAToTCache: MutableMap<KClass<*>, Cache<Any, Any>> = mutableMapOf()
    // tClass => ( i => t )
    private var globalIToTCache: MutableMap<KClass<*>, Cache<Any, Any>> = mutableMapOf()
    // eJMapper => ( i => ejm )
    private var globalIToEjmCache : MutableMap<(Collection<Any?>) -> Map<Any?, Any?>,
            MutableMap<Any?, Any?>> = mutableMapOf()

    fun <I, A> putGlobalIToACache(aClazz: KClass<*>, iToA: Map<I, A>) {
        val iToACache = globalIToACache
            .computeIfAbsent(aClazz) { mutableMapOf(null to null) }
        val aToICache = globalAToICache
            .computeIfAbsent(aClazz) { mutableMapOf(null to null) }
        iToA.forEach { (i, a) ->
            if (i != null) iToACache[i] = a
            if (a != null) aToICache[a] = i
        }
    }

    fun getGlobalIToACache(aClazz: KClass<*>, allI: Collection<Any?>) = globalIToACache
        .computeIfAbsent(aClazz) { mutableMapOf(null to null) }.filterKeys { allI.contains(it) }

    fun <T, A> putAllTToACache(tClazz: KClass<*>, tToA: Map<T, A>) {
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val aToICache = globalAToICache
            .computeIfAbsent(aClazz) { mutableMapOf(null to null) }
        val aToTCache = globalAToTCache
            .computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }
        val iToTCache = globalIToTCache
            .computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }

        tToA.forEach { (t, a) ->
            if (t != null && a != null) aToTCache.put(a, t)
            val i = aToICache[a]
            if (i != null && t != null) iToTCache.put(i, t)
        }
    }

    fun getGlobalAToTCache(tClazz: KClass<*>, allA: Collection<Any?>): Map<Any?, Any?> {
        val result = mutableMapOf<Any?, Any?>(null to null)
        val aToTCache = globalAToTCache
            .computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }
        allA.forEach { a -> if (a != null) {
            val t = aToTCache.getIfPresent(a)
            if (t != null) result[a] = t
        }}
        return result
    }

    fun getGlobalIToTCache(tClazz: KClass<*>, allI: Collection<Any?>): Map<Any?, Any?> {
        val result = mutableMapOf<Any?, Any?>(null to null)
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val iToACache = globalIToACache
            .computeIfAbsent(aClazz) { mutableMapOf(null to null) }
        val iToTCache = globalIToTCache
            .computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }
        allI.forEach { i ->  if (i != null) {
            val t = iToTCache.getIfPresent(i)
            if (t != null) {
                result[i] = t
                return@forEach
            }
            if (iToACache.containsKey(i)) {
                result[i] = null
                return@forEach
            }
        }}
        return result
    }

    fun <I, EJM> putAllIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>, iToEjmCache: Map<I, EJM>) {
        globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>)
        { mutableMapOf(null to null) }.putAll(iToEjmCache as Map<Any?, Any?>)
    }

    fun <I, EJM> getGlobalIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>, allI: Collection<Any?>) =
        globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>)
        { mutableMapOf(null to null) }.filterKeys { allI.contains(it) }

    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便缓存共享
            result.globalIToACache = from.globalIToACache
            result.globalAToICache = from.globalAToICache
            result.globalAToTCache = from.globalAToTCache
            result.globalIToTCache = from.globalIToTCache
            result.globalIToEjmCache = from.globalIToEjmCache
            return result
        }
    }
}