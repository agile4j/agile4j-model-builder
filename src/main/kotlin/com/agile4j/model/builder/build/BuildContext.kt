package com.agile4j.model.builder.build

import com.agile4j.model.builder.delegate.EJPDesc
import com.agile4j.model.builder.delegate.IJPDesc
import com.agile4j.model.builder.delegate.RDesc
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.utils.unifyTypeName
import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * abbreviations:
 * T        target
 * A        accompany
 * I        index
 * IJ       inJoin
 * IJI      inJoinIndex
 * @author liurenpeng
 * Created on 2020-06-17
 */
@Suppress("UNCHECKED_CAST")
internal object BuildContext {

    /**
     * TClass => AClass
     */
    val tToAHolder = ConcurrentHashMap<KClass<*>, KClass<*>>()

    /**
     * AClass => IClass
     */
    val aToIHolder = ConcurrentHashMap<KClass<*>, KClass<*>>()

    /**
     * AClass => (A) -> I
     */
    val indexerHolder = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * AClass => (Collection<I>) -> Map<I, A>
     */
    val builderHolder = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * AClass => IJClass =>  Set<(A) -> IJI>
     */
    val singleInJoinHolder = ConcurrentHashMap<KClass<*>, ConcurrentHashMap<KClass<*>, CopyOnWriteArraySet<Any>>>()

    /**
     * AClass => IJClass =>  Set<(A) -> Collection<IJI>>
     */
    val multiInJoinHolder = ConcurrentHashMap<KClass<*>, ConcurrentHashMap<KClass<*>, CopyOnWriteArraySet<Any>>>()

    fun getT(t: Type): KClass<Any>? = tToAHolder.keys
        .first { tKClazz -> tKClazz.java == t } as KClass<Any>
    fun getA(t: Type): KClass<Any>? = aToIHolder.keys
        .first { aKClazz -> aKClazz.java == t } as KClass<Any>

    fun assertCanBeT(c: KClass<*>) = if (isA(c) || isI(c) || c is Map<*, *> || c is Collection<*>)
        err("$this cannot be target.") else Unit
    fun assertCanBeA(c: KClass<*>) = if (isT(c) || isI(c) || c is Map<*, *> || c is Collection<*>)
        err("$this cannot be accompany.") else Unit
    fun assertCanBeI(c: KClass<*>) = if (isT(c) || isA(c) || c is Map<*, *> || c is Collection<*>)
        err("$this cannot be index.") else Unit

    fun isT(c: KClass<*>?) = c != null && tToAHolder.keys.contains(c)
    fun isT(t: Type?) = t != null && tToAHolder.keys.stream()
        .map { it.java.typeName }.map(::unifyTypeName).anyMatch{ it == unifyTypeName(t.typeName) }
    fun isI(c: KClass<*>?) = c != null && aToIHolder.values.contains(c)
    fun isI(t: Type?) = t != null && aToIHolder.values.stream()
        .map { it.java.typeName }.map(::unifyTypeName).anyMatch{ it == unifyTypeName(t.typeName) }
    fun isA(c: KClass<*>?) = c != null && aToIHolder.keys.contains(c)
    fun isA(t: Type?) = t != null && aToIHolder.keys.stream()
        .map { it.java.typeName }.map(::unifyTypeName).anyMatch{ it == unifyTypeName(t.typeName) }

    val iJPDescHolder = Caffeine.newBuilder().build<(Any) -> Any?, IJPDesc<Any, Any>> { IJPDesc(it) }
    val eJPDescHolder = Caffeine.newBuilder().build<(Collection<Any>) -> Map<Any, Any?>, EJPDesc<Any, Any>> { EJPDesc(it) }
    val rDescHolder = Caffeine.newBuilder().build<KProperty<*>, RDesc> { RDesc(it) }

}