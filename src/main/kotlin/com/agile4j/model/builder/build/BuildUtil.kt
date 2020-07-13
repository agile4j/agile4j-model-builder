package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
import com.agile4j.model.builder.accessor.JoinAccessor
import com.agile4j.model.builder.accessor.JoinTargetAccessor
import com.agile4j.model.builder.accessor.OutJoinAccessor
import com.agile4j.model.builder.accessor.OutJoinTargetAccessor
import com.agile4j.model.builder.build.AccompaniesAndTargetsDTO.Companion.emptyDTO
import com.agile4j.model.builder.build.AccompaniesAndTargetsDTO.Companion.getTargets
import com.agile4j.model.builder.build.AccompaniesAndTargetsDTO.Companion.isEmpty
import com.agile4j.model.builder.delegate.ITargetDelegate.ScopeKeys.modelBuilderScopeKey
import com.agile4j.model.builder.delegate.ModelBuilderDelegate
import com.agile4j.utils.util.ArrayUtil
import com.agile4j.utils.util.CollectionUtil
import com.agile4j.utils.util.MapUtil
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure

/**
 * @author liurenpeng
 * Created on 2020-07-09
 */

internal var Any.buildInModelBuilder : ModelBuilder by ModelBuilderDelegate()

/**
 * @param T target
 * @param IOA accompanyIndex or accompany
 */
fun <IOA, T : Any> buildTargets(
    buildMultiPair: BuildMultiPair<KClass<T>>,
    sources: Collection<IOA>
): Set<T> {
    val dto = buildAccompaniesAndTargets(buildMultiPair, sources)
    if (isEmpty(dto)) {
        return emptySet()
    }
    val targets = getTargets(dto)

    injectModelBuilder(buildMultiPair.modelBuilder, targets)
    injectAccompaniesAndTargets(buildMultiPair.modelBuilder, dto)
    injectRelation(targets)
    return targets
}

fun isTargetClass(property: KProperty<*>) : Boolean {
    val isCollection = Collection::class.java.isAssignableFrom(property.returnType.jvmErasure.java)
    if (!isCollection) {
        return isTargetClass(property.returnType.jvmErasure)
    }

    val type = property.javaGetter?.genericReturnType as? ParameterizedType
        ?: return isTargetClass(property.returnType.jvmErasure)
    val actualTypeArguments = type.actualTypeArguments
    if (ArrayUtil.isEmpty(actualTypeArguments)) {
        return false
    }
    val actualTypeArgumentType = actualTypeArguments[0]
    return isTargetClass(actualTypeArgumentType)
}

private fun isTargetClass(clazz: KClass<*>) : Boolean {
    return BuildContext.accompanyHolder.keys.contains(clazz)
}

private fun isTargetClass(type: Type) : Boolean {
    return BuildContext.accompanyHolder.keys.map { it.java }.contains(type)
}

private fun <IOA, T : Any> buildAccompaniesAndTargets(
    buildMultiPair: BuildMultiPair<KClass<T>>,
    sources: Collection<IOA>
): AccompaniesAndTargetsDTO<T> {
    if (CollectionUtil.isEmpty(sources)) {
        return emptyDTO()
    }
    val targetClazz = buildMultiPair.targetClazz
    if (!BuildContext.accompanyHolder.keys.contains(targetClazz)) {
        throw ModelBuildException("unregistered targetClass:$targetClazz")
    }

    val accompanyClazz = BuildContext.accompanyHolder[targetClazz]!!
    val indexToAccompanyMap : Map<out Any, Any> = buildAccompanyMap(accompanyClazz, sources)
    val targetAccompanyToMap = indexToAccompanyMap.values
        .map {buildTarget(targetClazz, accompanyClazz, it) to it}.toMap()
    return AccompaniesAndTargetsDTO(indexToAccompanyMap, targetAccompanyToMap)
}

data class AccompaniesAndTargetsDTO<T> (
    val indexToAccompanyMap: Map<out Any, Any>,
    val targetAccompanyToMap: Map<T, Any>
) {
    companion object {
        fun <T> emptyDTO() : AccompaniesAndTargetsDTO<T> = AccompaniesAndTargetsDTO(emptyMap(), emptyMap())
        fun <T> isEmpty(dto: AccompaniesAndTargetsDTO<T>) = MapUtil.isEmpty(dto.targetAccompanyToMap)
                || MapUtil.isEmpty(dto.indexToAccompanyMap)
        fun <T> getTargets(dto: AccompaniesAndTargetsDTO<T>) : Set<T> = dto.targetAccompanyToMap.keys.toSet()
    }
}

private fun <T : Any> buildTarget(
    targetClazz: KClass<T>,
    accompanyClazz: KClass<*>,
    accompany: Any
): T {
    return targetClazz.constructors.stream()
        .filter { it.parameters.size == 1 }
        .filter { it.parameters[0].type == accompanyClazz.createType() }
        .findFirst()
        .map { it.call(accompany) }
        .orElseThrow { ModelBuildException("no suitable constructor was found for targetClazz: $targetClazz") }
}

/**
 * @return accompanyIndex -> accompany
 */
@Suppress("UNCHECKED_CAST")
private fun <IOA> buildAccompanyMap(
    accompanyClazz: KClass<*>,
    indies: Collection<IOA>
): Map<out Any, Any> {
    return if (accompanyClazz.isInstance(indies.toList()[0])) {
        // buildByAccompany
        val accompanyIndexer = BuildContext.indexerHolder[accompanyClazz] as (IOA) -> Any
        indies.map { accompanyIndexer.invoke(it) to it }.toMap() as Map<out Any, Any>
    } else { // TODO 类型判断是否为accompanyIndex
        // buildByAccompanyIndex
        val accompanyBuilder = BuildContext.builderHolder[accompanyClazz] as (Collection<IOA>) -> Map<Any, Any>

        // 缓存中可能有，先从缓存查一下
        //val cached =
        if (modelBuilderScopeKey.get() != null
            && MapUtil.isNotEmpty(modelBuilderScopeKey.get()!!.joinCacheMap[accompanyClazz])) {
            val cache = modelBuilderScopeKey.get()!!.joinCacheMap[accompanyClazz]!!
            val cached = cache.filter { indies.contains(it.key as IOA) }
            val unCachedIndies = indies.filter { !cached.keys.contains(it as Any) }

            val buildMap = accompanyBuilder.invoke(unCachedIndies)

            val result = mutableMapOf<Any, Any>()
            result.putAll(buildMap)
            result.putAll(cached)
            result.toMap()
        } else {
            // 没缓存，全部直接查
            accompanyBuilder.invoke(indies)
        }
    }
}

private fun <T : Any> injectModelBuilder(
    modelBuilder: ModelBuilder,
    targets: Set<T>
) {
    targets.forEach { it.buildInModelBuilder = (modelBuilder) }
}

private fun <T : Any> injectAccompaniesAndTargets(
    modelBuilder: ModelBuilder,
    dto: AccompaniesAndTargetsDTO<T>
) {
    // TODO kv反转找个好用的现成工具类或自己写一个
    val accompanyToIndexMap = dto.indexToAccompanyMap.map { (k, v) -> v to k }.toMap()
    val accompanyToTargetMap = dto.targetAccompanyToMap.map { (k, v) -> v to k }.toMap()
    val indexToTargetMap = dto.indexToAccompanyMap
        .map{(i, a) -> i to accompanyToTargetMap[a]}
        .filter { it.second != null }
        .map {it.first to it.second!!}
        .map { it as Pair<Any, Any> }
        .toMap()
    val targetToIndexMap = indexToTargetMap.map { (k, v) -> v to k }.toMap()

    modelBuilder.indexToAccompanyMap.putAll(dto.indexToAccompanyMap)
    modelBuilder.accompanyToIndexMap.putAll(accompanyToIndexMap)
    modelBuilder.targetToAccompanyMap.putAll(dto.targetAccompanyToMap)
    modelBuilder.targetToIndexMap.putAll(targetToIndexMap)

    modelBuilder.joinTargetCacheMap.computeIfAbsent(modelBuilder.targetClazz()) {mutableMapOf()}
    modelBuilder.joinTargetCacheMap[modelBuilder.targetClazz()]!!.putAll(indexToTargetMap)
    modelBuilder.joinCacheMap.computeIfAbsent(modelBuilder.accompanyClazz()) {mutableMapOf()}
    modelBuilder.joinCacheMap[modelBuilder.accompanyClazz()]!!.putAll(dto.indexToAccompanyMap)

    //println("---${modelBuilder.joinTargetCacheMap}")
    //println("---${modelBuilder.joinCacheMap}")

}

private fun <T : Any> injectRelation(targets: Set<T>) {
    injectJoinAccessor(targets)
    injectJoinTargetAccessor(targets)
    injectOutJoinAccessor(targets)
    injectOutJoinTargetAccessor(targets)
}

private fun <T : Any> injectJoinAccessor(targets: Set<T>) {
    targets.forEach (::singleInjectJoinAccessor )
}

private fun <T : Any> singleInjectJoinAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.accompanyClazz()
    val joinAccessorMap = target.buildInModelBuilder.joinAccessorMap
    BuildContext.joinHolder[accompanyClazz]?.forEach { (joinClazz, _) ->
        joinAccessorMap[joinClazz] = JoinAccessor(joinClazz)
    }
}

private fun <T : Any> injectJoinTargetAccessor(targets: Set<T>) {
    targets.forEach (::singleInjectJoinTargetAccessor )
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> singleInjectJoinTargetAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.accompanyClazz()
    val joinTargetAccessorMap = target.buildInModelBuilder.joinTargetAccessorMap
    BuildContext.joinHolder[accompanyClazz]?.forEach { (joinClazz, _) ->
        var joinTargetClazz: KClass<*>? = BuildContext.accompanyHolder
            .map { (k, v) -> v to k }.toMap()[joinClazz] ?: return@forEach
        joinTargetClazz = joinTargetClazz as KClass<Any>
        joinTargetAccessorMap[joinTargetClazz] =
            JoinTargetAccessor(joinTargetClazz)
    }
}

private fun <T : Any> injectOutJoinAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetOutJoinAccessor )
}

private fun <T : Any> injectSingleTargetOutJoinAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.indexToAccompanyMap.values.elementAt(0)::class
    val outJoinAccessorMap = target.buildInModelBuilder.outJoinAccessorMap
    BuildContext.outJoinHolder[accompanyClazz]?.forEach { (outJoinPoint, _) ->
        outJoinAccessorMap[outJoinPoint] = OutJoinAccessor(outJoinPoint)
    }
}

private fun <T : Any> injectOutJoinTargetAccessor(targets: Set<T>) {
    targets.forEach (::injectSingleTargetOutJoinTargetAccessor )
}

private fun <T : Any> injectSingleTargetOutJoinTargetAccessor(target: T) {
    val accompanyClazz = target.buildInModelBuilder.indexToAccompanyMap.values.elementAt(0)::class
    val outJoinTargetAccessorMap = target.buildInModelBuilder.outJoinTargetAccessorMap
    BuildContext.outJoinHolder[accompanyClazz]?.forEach { (outJoinPoint, _) ->
        outJoinTargetAccessorMap[outJoinPoint] =
            OutJoinTargetAccessor(outJoinPoint)
    }
}

