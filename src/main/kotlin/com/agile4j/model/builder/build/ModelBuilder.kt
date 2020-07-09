package com.agile4j.model.builder.build

import com.agile4j.model.builder.accessor.JoinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor
import com.agile4j.model.builder.accessor.OutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor
import java.util.*
import kotlin.reflect.KClass

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */
class ModelBuilder {
    // accompanies and targets
    val targetToAccompanyMap: MutableMap<Any, Any> = WeakHashMap()
    val indexToAccompanyMap: MutableMap<Any, Any> = WeakHashMap()
    val accompanyToIndexMap: MutableMap<Any, Any> = WeakHashMap()

    // relations
    val joinAccessorMap : MutableMap<KClass<*>, JoinAccessor<Any, Any, Any>> = mutableMapOf()
    val joinTargetAccessorMap : MutableMap<KClass<*>, JoinTargetAccessor<Any, Any, Any>> = mutableMapOf()
    val outJoinAccessorMap : MutableMap<String, OutJoinAccessor<Any, Any, Any>> = mutableMapOf()
    val outJoinTargetAccessorMap : MutableMap<String, OutJoinTargetAccessor<Any, Any, Any>> = mutableMapOf()
}