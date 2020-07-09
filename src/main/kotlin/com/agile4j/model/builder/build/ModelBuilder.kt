package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
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
    /**
     * eg: movieView -> movie
     */
    val targetToAccompanyMap: MutableMap<Any, Any> = WeakHashMap()
    /**
     * eg: movie -> movieId
     */
    val accompanyToIndexMap: MutableMap<Any, Any> = WeakHashMap()
    /**
     * eg: movieId -> movie
     */
    val indexToAccompanyMap: MutableMap<Any, Any> = mutableMapOf()

    /**
     * joinClass -> joinAccessor<AccompanyClass, JoinIndexClass, JoinClass>
     * eg: User::class -> joinAccessor<Movie::class, Long::class, User::class>
     */
    val joinAccessorMap : MutableMap<KClass<*>, JoinAccessor<Any, Any, Any>> = mutableMapOf()
    val joinTargetAccessorMap : MutableMap<KClass<*>, JoinTargetAccessor<Any, Any, Any>> = mutableMapOf()
    val outJoinAccessorMap : MutableMap<String, OutJoinAccessor<Any, Any, Any>> = mutableMapOf()
    val outJoinTargetAccessorMap : MutableMap<String, OutJoinTargetAccessor<Any, Any, Any>> = mutableMapOf()

    fun accompanyClazz(): KClass<out Any> = indexToAccompanyMap.values.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }
}