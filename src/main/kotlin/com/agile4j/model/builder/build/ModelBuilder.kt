package com.agile4j.model.builder.build

import com.agile4j.model.builder.build.BuildContext.getAClazzByT
import com.agile4j.utils.util.CollectionUtil
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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

    private lateinit var currAllACache: MutableSet<Any>
    private lateinit var currAllICache: MutableSet<Any>

    private val currAToICache = Caffeine.newBuilder().build<Any, Any>()
    private val currIToACache = Caffeine.newBuilder().build<Any, Any>()
    private val currTToACache = Caffeine.newBuilder().weakKeys().build<Any, Any>()
    private val currAToTCache = Caffeine.newBuilder().weakValues().build<Any, Any>()
    private val currIToTCache = Caffeine.newBuilder().weakValues().build<Any, Any>()

    fun <T: Any> putCurrIAT(i2a: Map<Any, Any>, t2a: Map<T, Any>) {
        currAllACache = HashSet(i2a.values)
        currAllICache = HashSet(i2a.keys)

        currTToACache.putAll(t2a)
        i2a.forEach { (i, a) -> currAToICache.put(a, i) }
        i2a.forEach { (i, a) -> currIToACache.put(i, a) }
        t2a.forEach { (t, a) -> currAToTCache.put(a, t) }
        i2a.forEach { (i, a) -> currIToTCache.put(i, currAToTCache.get(a) { null }!!) }
    }

    fun <T: Any> removeByTColl(tColl: Collection<T>?) {
        if (CollectionUtil.isEmpty(tColl)) return
        val aColl = tColl!!.map { getCurrAByT(it) }
        val iColl = aColl.map { getCurrIByA(it) }

        currAllACache.removeAll(aColl)
        currAllICache.removeAll(iColl)
        currAToICache.invalidateAll(aColl)
        currIToACache.invalidateAll(iColl)
        currTToACache.invalidateAll(tColl)
        currAToTCache.invalidateAll(aColl)
        currIToTCache.invalidateAll(iColl)
    }

    fun getCurrAllA() = currAllACache.toSet()
    fun getCurrAllI() = currAllICache.toSet()
    fun getCurrAToT() = currAToTCache.asMap()
    fun getCurrIToT() = currIToTCache.asMap()
    fun getCurrAByI(i: Any?): Any? = currIToACache.get(i!!){ null }
    fun getCurrAByT(t: Any?): Any? = currTToACache.get(t!!){ null }
    fun getCurrIByA(a: Any?): Any? = currAToICache.get(a!!){ null }
    fun getCurrTByI(i: Any?): Any? = currIToTCache.get(i!!){ null }
    fun getCurrTByA(a: Any?): Any? = currAToTCache.get(a!!){ null }


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

    fun putGlobalIToACacheAndReturnUnNull(aClazz: KClass<*>, iToA: Map<Any?, Any?>): Map<Any, Any> {
        val iToACache = getGlobalIToACache(aClazz)
        val aToICache = getGlobalAToICache(aClazz)

        val result = mutableMapOf<Any, Any>()
        iToA.forEach { (i, a) ->
            if (i != null) iToACache[i] = a
            if (a != null) aToICache[a] = i
            if (i != null && a != null) result[i] = a
        }
        return result
    }

    fun getGlobalIToACache(aClazz: KClass<*>, allI: Collection<Any?>): CacheResp {
        val iToACache = getGlobalIToACache(aClazz)

        val cached = LinkedHashMap<Any?, Any?>()
        val unCached = mutableListOf<Any?>()
        allI.forEach { i -> if (iToACache.containsKey(i)) cached[i] = iToACache[i] else unCached.add(i) }
        return CacheResp(cached, unCached)
    }

    fun <T, A> putGlobalTToACache(tClazz: KClass<*>, tToA: Map<T, A>) {
        val aClazz = getAClazzByT(tClazz)!!
        val aToICache = getGlobalAToICache(aClazz)
        val aToTCache = getGlobalAToTCache(tClazz)
        val iToTCache = getGlobalIToTCache(tClazz)

        tToA.forEach { (t, a) ->
            if (t == null) return@forEach
            if (a != null) aToTCache.put(a, t)
            val i = aToICache[a]
            if (i != null) iToTCache.put(i, t)
        }
    }

    fun getGlobalAToTCache(tClazz: KClass<*>, allA: Collection<Any?>): CacheResp {
        val aToTCache = getGlobalAToTCache(tClazz)

        val cached = LinkedHashMap<Any?, Any?>()
        val unCached = mutableListOf<Any?>()
        allA.forEach { a ->
            if (a == null) {
                cached[null] = null
            } else {
                val t = aToTCache.getIfPresent(a)
                if (t != null) cached[a] = t else unCached.add(a)
            }
        }
        return CacheResp(cached, unCached)
    }

    fun getGlobalIToTCache(tClazz: KClass<*>, allI: Collection<Any?>): CacheResp {
        val aClazz = getAClazzByT(tClazz)!!
        val iToACache = getGlobalIToACache(aClazz)
        val iToTCache = getGlobalIToTCache(tClazz)

        val cached = LinkedHashMap<Any?, Any?>()
        val unCached = mutableListOf<Any?>()
        allI.forEach { i ->
            if (i == null) {
                cached[null] = null
            } else {
                val t = iToTCache.getIfPresent(i)
                if (t != null) {
                    cached[i] = t
                    return@forEach
                }
                if (iToACache.containsKey(i) && iToACache[i] == null) {
                    cached[i] = null
                    return@forEach
                }
                unCached.add(i)
            }
        }
        return CacheResp(cached, unCached)
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
    ): CacheResp {
        val iToEjmCache = getGlobalIToEjmCache(mapper)
        val cached = mutableMapOf<Any?, Any?>()
        val unCached = mutableListOf<Any?>()
        allI.forEach { i -> if (iToEjmCache.containsKey(i)) cached[i] = iToEjmCache[i] else unCached.add(i) }
        return CacheResp(cached, unCached)
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