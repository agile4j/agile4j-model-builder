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
internal object BuildContext {

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
    val indexerHolder = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * AClass => (Collection<I>) -> Map<I, A>
     */
    val builderHolder = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * AClass => IJClass =>  Set<(A) -> IJI>
     */
    private val singleInJoinHolder = ConcurrentHashMap<KClass<*>, ConcurrentHashMap<KClass<*>, CopyOnWriteArraySet<Any>>>()

    /**
     * AClass => IJClass =>  Set<(A) -> Collection<IJI>>
     */
    private val multiInJoinHolder = ConcurrentHashMap<KClass<*>, ConcurrentHashMap<KClass<*>, CopyOnWriteArraySet<Any>>>()


    /**
     * caches
     */
    private val tTypeNameToClass = ConcurrentHashMap<String, KClass<Any>>()
    private val aTypeNameToClass = ConcurrentHashMap<String, KClass<Any>>()
    private val iTypeNames = CopyOnWriteArraySet<String>()

    val iJPDescHolder = Caffeine.newBuilder().build<(Any) -> Any?, IJPDesc<Any, Any>> { IJPDesc(it) }
    val eJPDescHolder = Caffeine.newBuilder().build<(Collection<Any>) -> Map<Any, Any?>, EJPDesc<Any, Any>> { EJPDesc(it) }
    val rDescHolder = Caffeine.newBuilder().build<KProperty<*>, RDesc> { RDesc(it) }

    val constructorHolder = Caffeine.newBuilder().build<KClass<*>, KFunction<*>> { getConstructor(it, getAClazzByT(it)) }

    fun putTToA(tClazz: KClass<*>, aClazz: KClass<*>) {
        tToAHolder[tClazz] = aClazz
        tTypeNameToClass[unifyTypeName(tClazz.java.typeName)] = tClazz as KClass<Any>
    }

    fun putAToI(aClazz: KClass<*>, iClazz: KClass<*>) {
        aToIHolder[aClazz] = iClazz
        aTypeNameToClass[unifyTypeName(aClazz.java.typeName)] = aClazz as KClass<Any>
        iTypeNames.add(unifyTypeName(iClazz.java.typeName))
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
    fun isA(aClazz: KClass<*>?) = aClazz != null && aToIHolder.keys.contains(aClazz)
    fun isA(aType: Type?) = aType != null && aTypeNameToClass.keys.contains(unifyTypeName(aType.typeName))

    fun getSingleInJoinHolder(aClazz: KClass<*>) =
        singleInJoinHolder.computeIfAbsent(aClazz) { ConcurrentHashMap() }
    fun getMultiInJoinHolder(aClazz: KClass<*>) =
        multiInJoinHolder.computeIfAbsent(aClazz) { ConcurrentHashMap() }
}