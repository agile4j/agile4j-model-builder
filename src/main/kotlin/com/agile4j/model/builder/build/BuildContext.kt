package com.agile4j.model.builder.build

import com.agile4j.model.builder.delegate.EJPDesc
import com.agile4j.model.builder.delegate.IJPDesc
import com.agile4j.model.builder.delegate.RDesc
import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.utils.getConstructor
import com.agile4j.model.builder.utils.unifyTypeName
import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
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
object BuildContext {

    /**
     * TClass => AClass
     */
    private val tToAHolder = ConcurrentHashMap<KClass<*>, KClass<*>>()

    /**
     * AClass => IClass
     */
    private val aToIHolder = ConcurrentHashMap<KClass<*>, KClass<*>>()

    /**
     * AClass => (A) -> I
     */
    private val indexerHolder = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * AClass => (Collection<I>) -> Map<I, A>
     */
    private val builderHolder = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * AClass => IJClass =>  Set<(A) -> IJI>
     */
    private val singleInJoinHolder = ConcurrentHashMap<KClass<*>,
            ConcurrentHashMap<KClass<*>, CopyOnWriteArraySet<Any>>>()

    /**
     * AClass => IJClass =>  Set<(A) -> Collection<IJI>>
     */
    private val multiInJoinHolder = ConcurrentHashMap<KClass<*>,
            ConcurrentHashMap<KClass<*>, CopyOnWriteArraySet<Any>>>()

    /**
     * caches
     */
    private val tTypeNameToClass = ConcurrentHashMap<String, KClass<Any>>()
    private val aTypeNameToClass = ConcurrentHashMap<String, KClass<Any>>()
    private val iTypeNames = CopyOnWriteArraySet<String>()

    private val iJPDescHolder = Caffeine.newBuilder()
        .build<(Any) -> Any?, IJPDesc<Any, Any>> { IJPDesc(it) }
    private val eJPDescHolder = Caffeine.newBuilder()
        .build<(Collection<Any>) -> Map<Any, Any?>, EJPDesc<Any, Any>> { EJPDesc(it) }
    private val rDescHolder = Caffeine.newBuilder()
        .build<KProperty<*>, RDesc> { RDesc(it) }

    private val constructorHolder = Caffeine.newBuilder()
        .build<KClass<*>, KFunction<*>> { getConstructor(it, getAClazzByT(it)) }

    internal fun <T: Any> getConstructor(tClazz: KClass<T>): KFunction<T>? {
        return constructorHolder.get(tClazz) as KFunction<T>?
    }

    internal fun <A: Any, IJP: Any> getIJPDesc(mapper: (A) -> IJP?): IJPDesc<A, IJP> {
        return iJPDescHolder.get(mapper as (Any) -> Any?) as IJPDesc<A, IJP>
    }

    internal fun <I: Any, EJP: Any> getEJPDesc(mapper: (Collection<I>) -> Map<I, EJP?>): EJPDesc<I, EJP> {
        return eJPDescHolder.get(mapper as (Collection<Any>) -> Map<Any, Any?>) as EJPDesc<I, EJP>
    }

    internal fun getRDesc(property: KProperty<*>): RDesc {
        return rDescHolder.get(property)!!
    }

    internal fun putTToA(tClazz: KClass<*>, aClazz: KClass<*>) {
        tToAHolder[tClazz] = aClazz
        tTypeNameToClass[unifyTypeName(tClazz.java.typeName)] = tClazz as KClass<Any>
        aTypeNameToClass[unifyTypeName(aClazz.java.typeName)] = aClazz as KClass<Any>
    }

    internal fun putAToI(aClazz: KClass<*>, iClazz: KClass<*>) {
        aToIHolder[aClazz] = iClazz
        aTypeNameToClass[unifyTypeName(aClazz.java.typeName)] = aClazz as KClass<Any>
        iTypeNames.add(unifyTypeName(iClazz.java.typeName))
    }

    internal fun <A: Any, I> putIndexer(aClazz: KClass<*>, indexer: (A) -> I) {
        indexerHolder[aClazz] = indexer
    }

    internal fun <A: Any, I> putBuilder(aClazz: KClass<*>, builder: (Collection<I>) -> Map<I, A>) {
        builderHolder[aClazz] = builder
    }

    fun checkRelation(iClazz: Class<*>, aClazz: Class<*>, tClazz: Class<*>): Boolean {
        return checkRelation(iClazz.kotlin, aClazz.kotlin, tClazz.kotlin)
    }

    fun checkRelation(iClazz : KClass<*>, aClazz: KClass<*>, tClazz: KClass<*>): Boolean {
        return getAClazzByT(tClazz)?.equals(aClazz)?: false
                && getIClazzByA(aClazz)?.equals(iClazz)?: false
    }

    fun <A, I> getIndexer(aClazz: KClass<*>): (A) -> I {
        return indexerHolder[aClazz] as (A) -> I
    }

    fun <A, I> getBuilder(aClazz: KClass<*>): (Collection<I>) -> Map<I, A> {
        return builderHolder[aClazz] as (Collection<I>) -> Map<I, A>
    }

    fun getAClazzByT(tClazz: KClass<*>) = tToAHolder[tClazz]
    fun getIClazzByA(aClazz: KClass<*>) = aToIHolder[aClazz]

    fun getTClazzByType(tType: Type): KClass<Any>? = tTypeNameToClass[unifyTypeName(tType)]
    fun getAClazzByType(aType: Type): KClass<Any>? = aTypeNameToClass[unifyTypeName(aType)]

    fun assertCanBeT(c: KClass<*>) = if (isA(c) || isI(c) || c is Map<*, *> || c is Collection<*>)
        err("$this cannot be target.") else Unit
    fun assertCanBeA(c: KClass<*>) = if (isT(c) || isI(c) || c is Map<*, *> || c is Collection<*>)
        err("$this cannot be accompany.") else Unit
    fun assertCanBeI(c: KClass<*>) = if (isT(c) || isA(c) || c is Map<*, *> || c is Collection<*>)
        err("$this cannot be index.") else Unit

    fun isT(tClazz: KClass<*>?) = tClazz != null && tToAHolder.keys.contains(tClazz)
    fun isT(tType: Type?) = tType != null && tTypeNameToClass.keys.contains(unifyTypeName(tType.typeName))
    fun isI(iClazz: KClass<*>?) = iClazz != null && aToIHolder.values.contains(iClazz)
    fun isI(iType: Type?) = iType != null && iTypeNames.contains(unifyTypeName(iType.typeName))
    fun isA(aClazz: KClass<*>?) = aClazz != null
            && (aToIHolder.keys.contains(aClazz) || tToAHolder.values.contains(aClazz))
    fun isA(aType: Type?) = aType != null && aTypeNameToClass.keys.contains(unifyTypeName(aType.typeName))

    fun getSingleInJoinHolder(aClazz: KClass<*>) =
        singleInJoinHolder.computeIfAbsent(aClazz) { ConcurrentHashMap() }
    fun getMultiInJoinHolder(aClazz: KClass<*>) =
        multiInJoinHolder.computeIfAbsent(aClazz) { ConcurrentHashMap() }
}