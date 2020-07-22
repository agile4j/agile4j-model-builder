package com.agile4j.model.builder.build

import kotlin.reflect.KClass

/**
 * abbreviations:
 * T        target
 * A        accompany
 * AI       accompanyIndex
 * J        join
 * OJ       outJoin
 * OJM      outJoinModel
 * OJARM    outJoinAccompanyRelatedModel
 * OJX      if OutJoin OJM else if OutJoinTarget OJARM
 * @author liurenpeng
 * Created on 2020-06-17
 */
object BuildContext {

    /**
     * TClass => AClass
     */
    val accompanyHolder = mutableMapOf<KClass<*>, KClass<*>>()

    /**
     * AClass => (A) -> AI
     */
    val indexerHolder = mutableMapOf<KClass<*>, Any>()

    /**
     * AClass => (Collection<AI>) -> Map<AI, A>
     */
    val builderHolder = mutableMapOf<KClass<*>, Any>()

    /**
     * AClass => JClass =>  List<(A) -> JI>
     */
    val joinHolder = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Any>>>()

    /**
     * AClass => OJPoint => (Collection<AI>) -> Map<AI, OJX>
     * OJX: if OutJoin OJM else if OutJoinTarget OJARM
     */
    val outJoinHolder = mutableMapOf<KClass<*>, MutableMap<String, Any>>()
}