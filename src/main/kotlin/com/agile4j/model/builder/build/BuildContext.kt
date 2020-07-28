package com.agile4j.model.builder.build

import com.agile4j.model.builder.exception.ModelBuildException.Companion.err
import com.agile4j.model.builder.utils.unifyTypeName
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * abbreviations:
 * T        target
 * A        accompany
 * I        index
 * J        join
 * OJ       outJoin
 * OJM      outJoinModel
 * OJARM    outJoinAccompanyRelatedModel
 * OJX      if ExJoinDispatcherDelegate OJM else if OutJoinTarget OJARM
 * @author liurenpeng
 * Created on 2020-06-17
 */
@Suppress("UNCHECKED_CAST")
internal object BuildContext {

    /**
     * TClass => AClass
     */
    val tToAHolder = mutableMapOf<KClass<*>, KClass<*>>()

    /**
     * AClass => IClass
     */
    val aToIHolder = mutableMapOf<KClass<*>, KClass<*>>()

    /**
     * AClass => (A) -> I
     */
    val indexerHolder = mutableMapOf<KClass<*>, Any>()

    /**
     * AClass => (Collection<I>) -> Map<I, A>
     */
    val builderHolder = mutableMapOf<KClass<*>, Any>()

    /**
     * AClass => JClass =>  List<(A) -> JI>
     */
    val singleInJoinHolder = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableSet<Any>>>()

    /**
     * AClass => JClass =>  List<(A) -> Collection<JI>>
     */
    val multiInJoinHolder = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableSet<Any>>>()

    fun getT(t: Type): KClass<Any>? = if(!isT(t)) null else
        tToAHolder.keys.first { tKClazz -> tKClazz.java == t } as KClass<Any>
    fun getA(t: Type): KClass<Any>? = if(!isA(t)) null else
        aToIHolder.keys.first { aKClazz -> aKClazz.java == t } as KClass<Any>

    fun assertCanBeT(c: KClass<*>) = if (cannotBeT(c)) err("$this cannot be target.") else Unit
    fun assertCanBeA(c: KClass<*>) = if (cannotBeA(c)) err("$this cannot be accompany.") else Unit
    fun assertCanBeI(c: KClass<*>) = if (cannotBeI(c)) err("$this cannot be index.") else Unit

    fun isT(c: KClass<*>?) = c != null && tToAHolder.keys.contains(c)
    fun isT(t: Type?) = t != null && tToAHolder.keys
        .map { it.java.typeName }.map(::unifyTypeName).toSet().contains(unifyTypeName(t.typeName))
    fun isI(c: KClass<*>?) = c != null && aToIHolder.values.toSet().contains(c)
    fun isI(t: Type?) = t != null && aToIHolder.values
        .map { it.java.typeName }.map(::unifyTypeName).toSet().contains(unifyTypeName(t.typeName))
    fun isA(c: KClass<*>?) = c != null && tToAHolder.values.contains(c)
    fun isA(t: Type?) = t != null && tToAHolder.values
        .map { it.java.typeName }.map(::unifyTypeName).toSet().contains(unifyTypeName(t.typeName))

    private fun cannotBeT(c: KClass<*>) = isA(c) || isI(c) || c is Map<*, *> || c is Collection<*>
    private fun cannotBeA(c: KClass<*>) = isT(c) || isI(c) || c is Map<*, *> || c is Collection<*>
    private fun cannotBeI(c: KClass<*>) = isT(c) || isA(c) || c is Map<*, *> || c is Collection<*>

}