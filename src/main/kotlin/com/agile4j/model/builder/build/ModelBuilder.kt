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
     * joinTargetClass -> ( joinIndex -> joinTarget )
     */
    var joinTargetCacheMap: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()
    /**
     * joinClass -> ( joinIndex -> joinAccompany )
     */
    var joinCacheMap: MutableMap<KClass<*>, MutableMap<Any, Any>> = mutableMapOf()
    /**
     * outJoinPoint -> ( accompany -> joinTarget )
     * 注意：这里必须用accompany而不能是accompanyIndex，原因是后者区分度不够，例如Movie和User的id可能相同
     */
    var outJoinTargetCacheMap: MutableMap<String, MutableMap<Any, Any>> = mutableMapOf()
    /**
     * outJoinPoint -> ( accompany -> joinAccompany/joinModel )
     * 注意：这里必须用accompany而不能是accompanyIndex，原因是后者区分度不够，例如Movie和User的id可能相同
     */
    var outJoinCacheMap: MutableMap<String, MutableMap<Any, Any>> = mutableMapOf()


    /**
     * eg: movieView -> movie
     */
    val targetToAccompanyMap: MutableMap<Any, Any> = WeakHashMap()
    /**
     * eg: movieView -> movieId
     */
    val targetToIndexMap: MutableMap<Any, Any> = WeakHashMap()
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

    fun accompanyClazz(): KClass<out Any> = accompanyToIndexMap.keys.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("indexToAccompanyMap is empty") }

    fun targetClazz(): KClass<out Any> = targetToAccompanyMap.keys.stream().findAny()
        .map { it::class }.orElseThrow { ModelBuildException("targetToAccompanyMap is empty") }

    companion object {
        fun copyBy(from: ModelBuilder?): ModelBuilder {
            if (from == null) {
                return ModelBuilder()
            }
            val result = ModelBuilder()
            // 直接赋值，而非putAll，使用同一map对象，以便"同呼吸，共命运"
            result.joinTargetCacheMap = from.joinTargetCacheMap
            result.joinCacheMap = from.joinCacheMap
            result.outJoinTargetCacheMap = from.outJoinTargetCacheMap
            result.outJoinCacheMap = from.outJoinCacheMap
            return result
        }
    }
}