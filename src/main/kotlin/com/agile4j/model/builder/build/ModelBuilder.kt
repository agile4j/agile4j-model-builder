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

    lateinit var currAllA: Set<Any?>
    lateinit var currAllI: Set<Any?>
    lateinit var currAClazz: KClass<*>
    lateinit var currTClazz: KClass<*>

    private val currTToACache = Caffeine.newBuilder().weakKeys().build<Any?, Any?>()
    private val currAToICache = Caffeine.newBuilder().build<Any?, Any?>()
    private val currAToTCache = Caffeine.newBuilder().weakValues().build<Any?, Any?>()
    private val currIToTCache = Caffeine.newBuilder().weakValues().build<Any?, Any?>()

    val currAToT: Map<Any?, Any?> get() = currAToTCache.asMap()
    val currIToT: Map<Any?, Any?> get() = currIToTCache.asMap()

    fun putCurrIAT(i2a: Map<*, *>, t2a: Map<*, *>) {
        currAllA = HashSet(i2a.values)
        currAllI = HashSet(i2a.keys)
        currAClazz = i2a.values.stream().filter{ it != null }.map { it as Any }.findAny()
            .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }
        currTClazz = t2a.keys.stream().filter{ it != null }.map { it as Any }.findAny()
            .map { it::class }.orElseThrow { ModelBuildException("targetToAccompanyMap is empty") }

        currTToACache.putAll(t2a)
        i2a.forEach { (i, a) -> currAToICache.put(a!!, i!!) }
        t2a.forEach { (t, a) -> currAToTCache.put(a!!, t!!) }
        i2a.forEach { (i, a) -> currIToTCache.put(i!!, currAToTCache.get(a!!) { null }!!) }
    }

    fun getCurrAByT(t: Any?): Any? = currTToACache.get(t!!){ null }
    fun getCurrIByA(a: Any?): Any? = currAToICache.get(a!!){ null }

    /**
     * ----------global cache
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

    private fun getGlobalIToACache(aClazz: KClass<*>) =
        globalIToACache.computeIfAbsent(aClazz) { mutableMapOf(null to null) }

    private fun getGlobalAToICache(aClazz: KClass<*>) =
        globalAToICache.computeIfAbsent(aClazz) { mutableMapOf(null to null) }

    private fun getGlobalAToTCache(tClazz: KClass<*>) =
        globalAToTCache.computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }

    private fun getGlobalIToTCache(tClazz: KClass<*>) =
        globalIToTCache.computeIfAbsent(tClazz) { Caffeine.newBuilder().weakValues().build() }

    private fun <I, EJM> getGlobalIToEjmCache(mapper: (Collection<I>) -> Map<I, EJM>) =
        globalIToEjmCache.computeIfAbsent(mapper as (Collection<Any?>) -> Map<Any?, Any?>) { mutableMapOf(null to null) }

    fun <I, A> putGlobalIToACache(aClazz: KClass<*>, iToA: Map<I, A>) {
        val iToACache = getGlobalIToACache(aClazz)
        val aToICache = getGlobalAToICache(aClazz)

        iToA.forEach { (i, a) ->
            if (i != null) iToACache[i] = a
            if (a != null) aToICache[a] = i
        }
    }

    fun getGlobalIToACache(aClazz: KClass<*>, allI: Collection<Any?>): MutableMap<Any?, Any?> {
        val iToACache = getGlobalIToACache(aClazz)

        val result = mutableMapOf<Any?, Any?>()
        allI.forEach { i -> if (iToACache.containsKey(i)) result[i] = iToACache[i] }
        return result
    }

    fun <T, A> putGlobalTToACache(tClazz: KClass<*>, tToA: Map<T, A>) {
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val aToICache = getGlobalAToICache(aClazz)
        val aToTCache = getGlobalAToTCache(tClazz)
        val iToTCache = getGlobalIToTCache(tClazz)

        tToA.forEach { (t, a) ->
            if (t != null && a != null) aToTCache.put(a, t)
            val i = aToICache[a]
            if (i != null && t != null) iToTCache.put(i, t)
        }
    }

    fun getGlobalAToTCache(tClazz: KClass<*>, allA: Collection<Any?>): MutableMap<Any?, Any?> {
        val aToTCache = getGlobalAToTCache(tClazz)

        val result = mutableMapOf<Any?, Any?>()
        allA.forEach { a ->
            if (a == null) {
                result[null] = null
            } else {
                val t = aToTCache.getIfPresent(a)
                if (t != null) result[a] = t
            }
        }
        return result
    }

    fun getGlobalIToTCache(tClazz: KClass<*>, allI: Collection<Any?>): MutableMap<Any?, Any?> {
        val aClazz = BuildContext.tToAHolder[tClazz]!!
        val iToACache = getGlobalIToACache(aClazz)
        val iToTCache = getGlobalIToTCache(tClazz)

        val result = mutableMapOf<Any?, Any?>()
        allI.forEach { i ->
            if (i == null) {
                result[null] = null
            } else {
                val t = iToTCache.getIfPresent(i)
                if (t != null) {
                    result[i] = t
                    return@forEach
                }
                if (iToACache.containsKey(i)) {
                    result[i] = null
                    return@forEach
                }
            }
        }
        return result
    }

    fun <I, EJM> putGlobalIToEjmCache(
        mapper: (Collection<I>) -> Map<I, EJM>,
        iToEjm: Map<I, EJM>
    ) {
        getGlobalIToEjmCache(mapper).putAll(iToEjm as Map<Any?, Any?>)
    }

    fun <I, EJM> getGlobalIToEjmCache(
        mapper: (Collection<I>) -> Map<I, EJM>,
        allI: Collection<Any?>
    ): MutableMap<Any?, Any?> {
        val iToEjmCache = getGlobalIToEjmCache(mapper)
        val result = mutableMapOf<Any?, Any?>()
        allI.forEach { i -> if (iToEjmCache.containsKey(i)) result[i] = iToEjmCache[i] }
        return result
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
            result.globalAToTCache = from.globalAToTCache
            result.globalIToTCache = from.globalIToTCache
            result.globalIToEjmCache = from.globalIToEjmCache
            return result
        }
    }
}