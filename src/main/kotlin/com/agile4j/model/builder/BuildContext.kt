package com.agile4j.model.builder

import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-06-17
 */
object BuildContext {
    val accompanyHolder = mutableMapOf<KClass<*>, KClass<*>>()
    val indexerHolder = mutableMapOf<KClass<*>, Any>()
    val builderHolder = mutableMapOf<KClass<*>, Any>()
    val joinHolder = mutableMapOf<KClass<*>, MutableMap<KClass<*>, MutableList<Any>>>()
    val outJoinHolder = mutableMapOf<KClass<*>, MutableMap<String, Any>>()
}