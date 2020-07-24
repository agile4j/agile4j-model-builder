package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException.Companion.err
import com.agile4j.model.builder.enum.ModelFlag
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
 * OJX      if OutJoin OJM else if OutJoinTarget OJARM
 * @author liurenpeng
 * Created on 2020-06-17
 */
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
    val joinHolder = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Any>>>()

    /**
     * AClass => OJPoint => (Collection<I>) -> Map<I, OJX>
     * OJX: if OutJoin OJM else if OutJoinTarget OJARM
     */
    val outJoinHolder = mutableMapOf<KClass<*>, MutableMap<String, Any>>()

    fun assertCanBeT(c: KClass<*>) = if (cannotBeT(c))
        err("$this cannot be target. Because it's already registered as ${c.flag}") else Unit
    fun assertCanBeA(c: KClass<*>) = if (cannotBeA(c))
        err("$this cannot be accompany. Because it's already registered as ${c.flag}") else Unit
    fun assertCanBeI(c: KClass<*>) = if (cannotBeI(c))
        err("$this cannot be index. Because it's already registered as ${c.flag}") else Unit

    fun isT(c: KClass<*>) = tToAHolder.keys.contains(c)
    fun isT(t: Type) = tToAHolder.keys.map { it.java }.contains(t)
    private fun isA(c: KClass<*>) = tToAHolder.values.contains(c) || aToIHolder.keys.contains(c)
    private fun isI(c: KClass<*>) = aToIHolder.values.contains(c)

    private fun cannotBeT(c: KClass<*>) = isA(c) || isI(c)
    private fun cannotBeA(c: KClass<*>) = isT(c) || isI(c)
    private fun cannotBeI(c: KClass<*>) = isT(c) || isA(c)

    private val KClass<*>.flag get(): ModelFlag = when {
        isT(this) -> ModelFlag.Target
        isA(this) -> ModelFlag.Accompany
        isI(this) -> ModelFlag.Index
        else -> ModelFlag.Other
    }

}