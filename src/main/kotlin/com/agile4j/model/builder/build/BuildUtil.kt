package com.agile4j.model.builder.build

import com.agile4j.model.builder.ModelBuildException
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
    val accompanyToTargetMap = indexToAccompanyMap.values
        .map { accompany ->  buildTarget(targetClazz, accompanyClazz, accompany) to accompany }.toMap()
    return AccompaniesAndTargetsDTO(indexToAccompanyMap, accompanyToTargetMap)
}

data class AccompaniesAndTargetsDTO<T> (
    val indexToAccompanyMap: Map<out Any, Any>,
    val targetToAccompanyMap: Map<T, Any>
) {

    companion object {
        fun <T> emptyDTO() : AccompaniesAndTargetsDTO<T> = AccompaniesAndTargetsDTO(emptyMap(), emptyMap())
        fun <T> isEmpty(dto: AccompaniesAndTargetsDTO<T>) = MapUtil.isEmpty(dto.targetToAccompanyMap)
                || MapUtil.isEmpty(dto.indexToAccompanyMap)
        fun <T> getTargets(dto: AccompaniesAndTargetsDTO<T>) : Set<T> = dto.targetToAccompanyMap.keys.toSet()
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
        val modelBuilder = modelBuilderScopeKey.get()
        val joinCacheMap = modelBuilder?.getJoinCacheMap(accompanyClazz)
        if (modelBuilder != null && MapUtil.isNotEmpty(joinCacheMap)) {
            // 有缓存
            val cached = joinCacheMap!!.filter { indies.contains(it.key as IOA) }
            val unCachedIndies = indies.filter { !cached.keys.contains(it as Any) }

            val result = mutableMapOf<Any, Any>()
            result.putAll(cached)
            if (CollectionUtil.isNotEmpty(unCachedIndies)) {
                result.putAll(accompanyBuilder.invoke(unCachedIndies))
            }
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
    modelBuilder.indexToAccompanyMap.putAll(dto.indexToAccompanyMap)
    modelBuilder.targetToAccompanyMap.putAll(dto.targetToAccompanyMap)

    modelBuilder.putAllJoinCacheMap(modelBuilder.accompanyClazz, modelBuilder.indexToAccompanyMap)
    modelBuilder.putAllJoinTargetCacheMap(modelBuilder.targetClazz, modelBuilder.indexToTargetMap)

}

